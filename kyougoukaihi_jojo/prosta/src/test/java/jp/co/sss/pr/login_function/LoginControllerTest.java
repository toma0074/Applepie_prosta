package jp.co.sss.pr.login_function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;

import jp.co.sss.pr.bean.CustomerBean;
import jp.co.sss.pr.entity.Customer;
import jp.co.sss.pr.form.CustomerForm;
import jp.co.sss.pr.repository.CustomerRepository;
import jp.co.sss.pr.service.LevelService;

@ExtendWith(MockitoExtension.class)
class LoginControllerTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private HttpSession session;

    @Mock
    private LevelService levelService;

    @Mock
    private jakarta.servlet.http.HttpServletRequest request;

    @org.mockito.Spy
    private jp.co.sss.pr.service.PasswordService passwordService = new jp.co.sss.pr.service.PasswordService();

    @InjectMocks
    private LoginController loginController;

    @Test
    void upgradesLegacyPlainPasswordToHashOnLogin() {
        CustomerForm form = new CustomerForm();
        form.setUserName("testuser");
        form.setUserPass("pass1234");
        Customer customer = activeCustomer(1);
        customer.setBackgroundClass("bg-color");
        customer.setBackgroundColor("#AABBCC");
        BindingResult bindingResult = mock(BindingResult.class);
        Model model = mock(Model.class);
        when(customerRepository.findAllByUserName("testuser")).thenReturn(List.of(customer));
        when(levelService.processLogin(customer)).thenReturn(List.of());

        loginController.login(form, bindingResult, session, model);

        assertEquals(true, passwordService.matches("pass1234", customer.getUserPass()));
        assertEquals(false, "pass1234".equals(customer.getUserPass()));
        verify(customerRepository).save(customer);
    }

    @Test
    void keepsLegacyPasswordAndStillLogsInWhenHashDoesNotFitTheColumn() {
        CustomerForm form = new CustomerForm();
        form.setUserName("testuser");
        form.setUserPass("pass1234");
        Customer customer = activeCustomer(1);
        BindingResult bindingResult = mock(BindingResult.class);
        Model model = mock(Model.class);
        when(customerRepository.findAllByUserName("testuser")).thenReturn(List.of(customer));
        when(customerRepository.save(customer)).thenThrow(new RuntimeException("ORA-12899"));
        when(levelService.processLogin(customer)).thenReturn(List.of());

        assertEquals("redirect:/mypage", loginController.login(form, bindingResult, session, model));
        assertEquals("pass1234", customer.getUserPass());
    }

    @Test
    void rejectsWrongPassword() {
        CustomerForm form = new CustomerForm();
        form.setUserName("testuser");
        form.setUserPass("wrong");
        BindingResult bindingResult = mock(BindingResult.class);
        Model model = mock(Model.class);
        when(customerRepository.findAllByUserName("testuser")).thenReturn(List.of(activeCustomer(1)));

        assertEquals("login/login", loginController.login(form, bindingResult, session, model));
        verify(session, never()).setAttribute(eq("user"), any());
    }

    @Test
    void showsHelpfulErrorWhenCredentialsMatchMultipleActiveCustomers() {
        CustomerForm form = new CustomerForm();
        form.setUserName("testuser");
        form.setUserPass("pass1234");
        Customer firstCustomer = activeCustomer(1);
        Customer secondCustomer = activeCustomer(2);
        BindingResult bindingResult = mock(BindingResult.class);
        Model model = mock(Model.class);
        when(customerRepository.findAllByUserName("testuser"))
                .thenReturn(List.of(firstCustomer, secondCustomer));
        when(bindingResult.hasErrors()).thenReturn(false);

        String view = loginController.login(form, bindingResult, session, model);

        assertEquals("login/login", view);
        verify(model).addAttribute("errMessage",
                "同じログイン情報のユーザーが複数登録されています。管理者にお問い合わせください。");
        verify(session, never()).setAttribute(eq("user"), any());
        verify(levelService, never()).processLogin(any(Customer.class));
    }

    @Test
    void logsInWhenOnlyOneMatchingCustomerIsActive() {
        CustomerForm form = new CustomerForm();
        form.setUserName("testuser");
        form.setUserPass("pass1234");
        Customer activeCustomer = activeCustomer(1);
        activeCustomer.setBackgroundClass("bg-color");
        activeCustomer.setBackgroundColor("#AABBCC");
        Customer deletedCustomer = activeCustomer(2);
        deletedCustomer.setDeleteFlag(true);
        BindingResult bindingResult = mock(BindingResult.class);
        Model model = mock(Model.class);
        when(customerRepository.findAllByUserName("testuser"))
                .thenReturn(List.of(activeCustomer, deletedCustomer));
        when(bindingResult.hasErrors()).thenReturn(false);
        when(levelService.processLogin(activeCustomer)).thenReturn(List.of());

        String view = loginController.login(form, bindingResult, session, model);

        assertEquals("redirect:/mypage", view);
        verify(session).setAttribute(eq("user"), any());
        verify(session).setAttribute("backgroundClass", "bg-color");
        verify(session).setAttribute("backgroundColor", "#AABBCC");
    }

    @Test
    void registersNewUserWithRequiredInitialLoginFields() {
        CustomerForm form = new CustomerForm();
        form.setUserName("newuser");
        form.setUserPass("pass1234");
        BindingResult bindingResult = mock(BindingResult.class);
        Model model = mock(Model.class);
        when(customerRepository.findByUserName("newuser")).thenReturn(null);
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> {
            Customer customer = invocation.getArgument(0);
            customer.setUserId(4);
            return customer;
        });

        String view = loginController.checkRegist(form, bindingResult, model);

        assertEquals("redirect:/mypage", view);
        verify(customerRepository).save(argThat(customer ->
                customer.getLoginDate() != null
                        && customer.getLoginCount() == 1
                        && customer.getLoginFlag()
                        && customer.getConsecutiveLoginDays() == 1
                        && customer.getLastSeenAt() != null));
        verify(levelService, never()).processLogin(any(Customer.class));
        verify(session).setAttribute(eq("user"), any(CustomerBean.class));
    }

    private Customer activeCustomer(int userId) {
        Customer customer = new Customer();
        customer.setUserId(userId);
        customer.setUserName("testuser");
        customer.setUserPass("pass1234");
        customer.setPermission(0);
        customer.setDeleteFlag(false);
        return customer;
    }
}
