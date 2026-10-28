package jp.co.sss.pr.mypage_function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.web.server.ResponseStatusException;

import jp.co.sss.pr.bean.CustomerBean;
import jp.co.sss.pr.entity.Customer;
import jp.co.sss.pr.repository.CustomerRepository;

@ExtendWith(MockitoExtension.class)
class BackgroundControllerTest {

    @Mock
    private CustomerRepository customerRepository;

    private BackgroundController controller;
    private Customer customer;
    private MockHttpSession session;

    @BeforeEach
    void setUp() {
        controller = new BackgroundController();
        org.springframework.test.util.ReflectionTestUtils.setField(
                controller, "customerRepository", customerRepository);
        customer = new Customer();
        customer.setUserId(7);
        customer.setUserLevel(50);
        CustomerBean user = new CustomerBean();
        user.setUserId(7);
        session = new MockHttpSession();
        session.setAttribute("user", user);
    }

    @Test
    void savesImageBackgroundToCustomerAndSession() {
        when(customerRepository.findById(7)).thenReturn(Optional.of(customer));
        assertEquals("redirect:/background",         controller.change("bg-sky", session));

        verify(customerRepository).save(argThat(saved ->
                "bg-sky".equals(saved.getBackgroundClass())
                        && saved.getBackgroundColor() == null));
        assertEquals("bg-sky", session.getAttribute("backgroundClass"));
        org.junit.jupiter.api.Assertions.assertNull(session.getAttribute("backgroundColor"));
    }

    @Test
    void savesCustomColorToCustomerAndSession() {
        when(customerRepository.findById(7)).thenReturn(Optional.of(customer));
        assertEquals("redirect:/background", controller.changeColor("#a1B2c3", session));

        verify(customerRepository).save(argThat(saved ->
                "bg-color".equals(saved.getBackgroundClass())
                        && "#A1B2C3".equals(saved.getBackgroundColor())));
        assertEquals("bg-color", session.getAttribute("backgroundClass"));
        assertEquals("#A1B2C3", session.getAttribute("backgroundColor"));
    }

    @Test
    void rejectsLockedBackgroundWithoutSaving() {
        when(customerRepository.findById(7)).thenReturn(Optional.of(customer));
        customer.setUserLevel(24);

        assertThrows(ResponseStatusException.class,
                () -> controller.change("bg-grass", session));
        verify(customerRepository, org.mockito.Mockito.never())
                .save(org.mockito.ArgumentMatchers.any(Customer.class));
    }

    @Test
    void rejectsInvalidCustomColorWithoutSaving() {
        assertThrows(ResponseStatusException.class,
                () -> controller.changeColor("red", session));
        verify(customerRepository, org.mockito.Mockito.never())
                .save(org.mockito.ArgumentMatchers.any(Customer.class));
    }
}
