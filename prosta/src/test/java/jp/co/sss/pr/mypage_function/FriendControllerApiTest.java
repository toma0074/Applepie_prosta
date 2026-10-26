package jp.co.sss.pr.mypage_function;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import jp.co.sss.pr.bean.CustomerBean;
import jp.co.sss.pr.entity.Category;
import jp.co.sss.pr.entity.Customer;
import jp.co.sss.pr.entity.FriendConnection;
import jp.co.sss.pr.repository.AnswerRepository;
import jp.co.sss.pr.repository.CategoryRepository;
import jp.co.sss.pr.repository.CustomerRepository;
import jp.co.sss.pr.repository.FriendConnectionRepository;
import jp.co.sss.pr.repository.FriendStudySummary;

@ExtendWith(MockitoExtension.class)
class FriendControllerApiTest {

    @Mock
    private FriendConnectionRepository friendConnectionRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private AnswerRepository answerRepository;

    @Mock
    private CategoryRepository categoryRepository;

    private MockMvc mockMvc;
    private MockHttpSession session;
    private Customer currentUser;
    private Customer friend;
    private FriendConnection acceptedConnection;

    @BeforeEach
    void setUp() {
        currentUser = customer(1, "りんご");
        friend = customer(2, "みかん");
        CustomerBean userBean = new CustomerBean();
        userBean.setUserId(currentUser.getUserId());
        session = new MockHttpSession();
        session.setAttribute("user", userBean);

        FriendController controller = new FriendController(
                friendConnectionRepository,
                customerRepository,
                answerRepository,
                categoryRepository);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        acceptedConnection = connection(currentUser, friend, currentUser, FriendConnection.ACCEPTED);
    }

    @Test
    void returnsFriendSummaryAsScreenConsumableJson() throws Exception {
        when(customerRepository.findById(1)).thenReturn(Optional.of(currentUser));
        when(friendConnectionRepository.findAllForUserAndStatus(
                currentUser, FriendConnection.ACCEPTED)).thenReturn(List.of(acceptedConnection));
        friend.setConsecutiveLoginDays(5);
        friend.setLastSeenAt(LocalDateTime.now().minusMinutes(1));
        FriendStudySummary friendStudy = summary(2, 12, 9);
        when(answerRepository.summarizeStudy(anyList(), isNull(), isNull(), isNull()))
                .thenReturn(List.of(friendStudy));

        mockMvc.perform(get("/api/friends").session(session))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$[0].userId").value(2))
                .andExpect(jsonPath("$[0].userName").value("みかん"))
                .andExpect(jsonPath("$[0].online").value(true))
                .andExpect(jsonPath("$[0].consecutiveLoginDays").value(5))
                .andExpect(jsonPath("$[0].answerCount").value(12))
                .andExpect(jsonPath("$[0].correctCount").value(9));
    }

    @Test
    void createsFriendRequestAndReturnsRequestData() throws Exception {
        when(customerRepository.findById(1)).thenReturn(Optional.of(currentUser));
        when(customerRepository.findById(2)).thenReturn(Optional.of(friend));
        when(friendConnectionRepository.findByUserLowAndUserHigh(currentUser, friend))
                .thenReturn(Optional.empty());
        when(friendConnectionRepository.save(any(FriendConnection.class))).thenAnswer(invocation -> {
            FriendConnection saved = invocation.getArgument(0);
            saved.setFriendConnectionId(55);
            return saved;
        });

        mockMvc.perform(post("/api/friends/requests/2").session(session))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.requestId").value(55))
                .andExpect(jsonPath("$.userId").value(2))
                .andExpect(jsonPath("$.userName").value("みかん"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void acceptsIncomingRequestAndReturnsNoContent() throws Exception {
        MockHttpSession receiverSession = sessionFor(2);
        FriendConnection incoming = connection(currentUser, friend, currentUser, FriendConnection.PENDING);
        incoming.setFriendConnectionId(55);
        when(customerRepository.findById(2)).thenReturn(Optional.of(friend));
        when(friendConnectionRepository.findById(55)).thenReturn(Optional.of(incoming));

        mockMvc.perform(post("/api/friends/requests/55/accept").session(receiverSession))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        org.junit.jupiter.api.Assertions.assertEquals(FriendConnection.ACCEPTED, incoming.getStatus());
        verify(friendConnectionRepository).save(incoming);
    }

    @Test
    void returnsCategoryLeaderboardForCurrentUserAndAcceptedFriends() throws Exception {
        when(customerRepository.findById(1)).thenReturn(Optional.of(currentUser));
        when(friendConnectionRepository.findAllForUserAndStatus(
                currentUser, FriendConnection.ACCEPTED)).thenReturn(List.of(acceptedConnection));
        Category category = new Category();
        category.setCategoryId(1);
        category.setCategoryName("Java");
        when(categoryRepository.findById(1)).thenReturn(Optional.of(category));
        FriendStudySummary currentStudy = summary(1, 10, 5);
        FriendStudySummary friendStudy = summary(2, 8, 6);
        when(answerRepository.summarizeStudy(
                anyList(), eq(1), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(currentStudy, friendStudy));

        mockMvc.perform(get("/api/friends/leaderboard")
                        .param("categoryId", "1")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.categoryId").value(1))
                .andExpect(jsonPath("$.categoryName").value("Java"))
                .andExpect(jsonPath("$.entries.length()").value(2))
                .andExpect(jsonPath("$.entries[0].userId").value(2))
                .andExpect(jsonPath("$.entries[0].answerCount").value(8))
                .andExpect(jsonPath("$.entries[0].correctCount").value(6))
                .andExpect(jsonPath("$.entries[0].accuracy").value(75.0))
                .andExpect(jsonPath("$.entries[1].userId").value(1))
                .andExpect(jsonPath("$.entries[1].accuracy").value(50.0));
    }

    @Test
    void rejectsFriendApiRequestWithoutLogin() throws Exception {
        mockMvc.perform(get("/api/friends"))
                .andExpect(status().isUnauthorized());
    }

    private MockHttpSession sessionFor(int userId) {
        CustomerBean userBean = new CustomerBean();
        userBean.setUserId(userId);
        MockHttpSession userSession = new MockHttpSession();
        userSession.setAttribute("user", userBean);
        return userSession;
    }

    private Customer customer(int userId, String userName) {
        Customer customer = new Customer();
        customer.setUserId(userId);
        customer.setUserName(userName);
        customer.setPermission(0);
        customer.setDeleteFlag(false);
        customer.setConsecutiveLoginDays(0);
        return customer;
    }

    private FriendConnection connection(
            Customer low, Customer high, Customer requester, String status) {
        FriendConnection connection = new FriendConnection();
        connection.setUserLow(low);
        connection.setUserHigh(high);
        connection.setRequestedBy(requester);
        connection.setStatus(status);
        connection.setCreatedAt(LocalDateTime.now());
        connection.setUpdatedAt(LocalDateTime.now());
        return connection;
    }

    private FriendStudySummary summary(int userId, long answerCount, long correctCount) {
        FriendStudySummary summary = mock(FriendStudySummary.class);
        when(summary.getUserId()).thenReturn(userId);
        when(summary.getAnswerCount()).thenReturn(answerCount);
        when(summary.getCorrectCount()).thenReturn(correctCount);
        return summary;
    }
}
