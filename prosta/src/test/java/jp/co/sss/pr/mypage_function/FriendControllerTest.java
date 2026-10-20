package jp.co.sss.pr.mypage_function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.web.server.ResponseStatusException;

import jp.co.sss.pr.bean.CustomerBean;
import jp.co.sss.pr.entity.Customer;
import jp.co.sss.pr.entity.FriendConnection;
import jp.co.sss.pr.repository.AnswerRepository;
import jp.co.sss.pr.repository.CategoryRepository;
import jp.co.sss.pr.repository.CustomerRepository;
import jp.co.sss.pr.repository.FriendConnectionRepository;

@ExtendWith(MockitoExtension.class)
class FriendControllerTest {

    @Mock
    private FriendConnectionRepository friendConnectionRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private AnswerRepository answerRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private FriendController friendController;

    private MockHttpSession session;
    private Customer currentUser;
    private FriendConnection request;

    @BeforeEach
    void setUp() {
        currentUser = customer(3);
        CustomerBean userBean = new CustomerBean();
        userBean.setUserId(currentUser.getUserId());
        session = new MockHttpSession();
        session.setAttribute("user", userBean);
        request = new FriendConnection();
        request.setFriendConnectionId(7);
        request.setUserLow(customer(1));
        request.setUserHigh(customer(2));
        request.setRequestedBy(request.getUserLow());
        request.setStatus(FriendConnection.PENDING);
        when(friendConnectionRepository.findById(7)).thenReturn(Optional.of(request));
    }

    @Test
    void userOutsideRequestCannotAcceptIt() {
        when(customerRepository.findById(currentUser.getUserId()))
                .thenReturn(Optional.of(currentUser));
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> friendController.acceptRequest(7, session));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verify(friendConnectionRepository, never()).save(request);
    }

    @Test
    void requesterCannotAcceptTheirOwnRequest() {
        session.setAttribute("user", userBean(1));
        when(customerRepository.findById(1)).thenReturn(Optional.of(request.getUserLow()));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> friendController.acceptRequest(7, session));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        verify(friendConnectionRepository, never()).save(request);
    }

    private Customer customer(int userId) {
        Customer customer = new Customer();
        customer.setUserId(userId);
        customer.setUserName("user" + userId);
        customer.setPermission(1);
        customer.setDeleteFlag(false);
        return customer;
    }

    private CustomerBean userBean(int userId) {
        CustomerBean userBean = new CustomerBean();
        userBean.setUserId(userId);
        return userBean;
    }
}
