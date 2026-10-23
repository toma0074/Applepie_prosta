package jp.co.sss.pr.login_function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.any;
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

    @InjectMocks
    private LoginController loginController;

    @Test
    void showsHelpfulErrorWhenCredentialsMatchMultipleActiveCustomers() {
        CustomerForm form = new CustomerForm();
        form.setUserName("testuser");
        form.setUserPass("pass1234");
        Customer firstCustomer = activeCustomer(1);
        Customer secondCustomer = activeCustomer(2);
        BindingResult bindingResult = mock(BindingResult.class);
        Model model = mock(Model.class);
        when(customerRepository.findAllByUserNameAndUserPass("testuser", "pass1234"))
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
        Customer deletedCustomer = activeCustomer(2);
        deletedCustomer.setDeleteFlag(true);
        BindingResult bindingResult = mock(BindingResult.class);
        Model model = mock(Model.class);
        when(customerRepository.findAllByUserNameAndUserPass("testuser", "pass1234"))
                .thenReturn(List.of(activeCustomer, deletedCustomer));
        when(bindingResult.hasErrors()).thenReturn(false);
        when(levelService.processLogin(activeCustomer)).thenReturn(List.of());

        String view = loginController.login(form, bindingResult, session, model);

        assertEquals("redirect:/mypage", view);
        verify(session).setAttribute(eq("user"), any());
    }

    private Customer activeCustomer(int userId) {
        Customer customer = new Customer();
        customer.setUserId(userId);
        customer.setUserName("testuser");
        customer.setUserPass("pass1234");
        customer.setPermission(1);
        customer.setDeleteFlag(false);
        return customer;
    }
}
