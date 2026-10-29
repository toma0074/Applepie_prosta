package jp.co.sss.pr.user_function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import jakarta.servlet.http.HttpSession;
import jp.co.sss.pr.bean.CustomerBean;
import jp.co.sss.pr.entity.Customer;
import jp.co.sss.pr.form.CustomerForm;
import jp.co.sss.pr.repository.CustomerRepository;
import jp.co.sss.pr.service.PasswordService;

@ExtendWith(MockitoExtension.class)
class UserControllerPendingPasswordTest {

    @Mock
    private CustomerRepository userRepository;

    @Mock
    private HttpSession session;

    private final Map<String, Object> attrs = new HashMap<>();
    private UserController controller;
    private Customer target;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        controller = new UserController();
        controller.userRepository = userRepository;
        controller.passwordService = new PasswordService();
        controller.session = session;
        controller.validator = validator;

        CustomerBean login = new CustomerBean();
        login.setUserId(1);
        login.setUserName("taro");
        login.setPermission(0);
        target = new Customer();
        target.setUserId(1);
        target.setUserName("taro");
        target.setPermission(0);

        lenient().when(session.getAttribute("user")).thenReturn(login);
        lenient().when(session.getAttribute("pendingUserPass")).thenAnswer(i -> attrs.get("pendingUserPass"));
        lenient().when(session.getAttribute("pendingUserId")).thenAnswer(i -> attrs.get("pendingUserId"));
        lenient().doAnswer(i -> attrs.put(i.getArgument(0), i.getArgument(1)))
                .when(session).setAttribute(any(), any());
        lenient().doAnswer(i -> attrs.remove((String) i.getArgument(0)))
                .when(session).removeAttribute(any());
        lenient().when(userRepository.findByUserId(1)).thenReturn(target);
    }

    private CustomerForm form(String pass) {
        CustomerForm form = new CustomerForm();
        form.setUserId(1);
        form.setUserName("taro");
        form.setUserPass(pass);
        form.setPermission(0);
        return form;
    }

    @Test
    void checkKeepsPasswordInSession() {
        CustomerForm form = form("pass1234");
        BindingResult result = new BeanPropertyBindingResult(form, "user");

        String view = controller.userUpdateCheck(form, result, new ExtendedModelMap());

        assertEquals("user/user_update_check", view);
        assertEquals("pass1234", attrs.get("pendingUserPass"));
    }

    @Test
    void completeUsesSessionPasswordAndClearsIt() {
        attrs.put("pendingUserPass", "pass1234");
        attrs.put("pendingUserId", 1);
        CustomerForm form = form(null);
        BindingResult result = new BeanPropertyBindingResult(form, "user");

        String view = controller.userUpdateComplete(form, result, new ExtendedModelMap());

        assertEquals("user/user_update_complete", view);
        assertNull(attrs.get("pendingUserPass"));
        verify(userRepository).save(target);
    }

    @Test
    void completeWithoutPendingPasswordRedirectsToInput() {
        CustomerForm form = form("pass1234");
        BindingResult result = new BeanPropertyBindingResult(form, "user");

        String view = controller.userUpdateComplete(form, result, new ExtendedModelMap());

        assertEquals("redirect:/user/update/input?userId=1&returnUrl=/mypage", view);
        verify(userRepository, never()).save(any());
    }
}
