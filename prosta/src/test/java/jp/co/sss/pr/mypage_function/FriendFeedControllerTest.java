package jp.co.sss.pr.mypage_function;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDate;
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
import jp.co.sss.pr.entity.*;
import jp.co.sss.pr.repository.*;

@ExtendWith(MockitoExtension.class)
class FriendFeedControllerTest {
    @Mock CustomerRepository customers;
    @Mock FriendConnectionRepository connections;
    @Mock AnswerRepository answers;
    @Mock UserAchievementRepository achievements;
    MockMvc mvc;
    MockHttpSession session;
    Customer user;
    Customer friend;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(
                new FriendFeedController(customers, connections, answers, achievements)).build();
        user = customer(1);
        friend = customer(2);
        session = new MockHttpSession();
        CustomerBean bean = new CustomerBean();
        bean.setUserId(1);
        session.setAttribute("user", bean);
    }

    private Customer customer(int id) {
        Customer c = new Customer();
        c.setUserId(id);
        c.setPermission(0);
        c.setDeleteFlag(false);
        return c;
    }

    private void authenticated() {
        when(customers.findById(1)).thenReturn(Optional.of(user));
        when(customers.findById(2)).thenReturn(Optional.of(friend));
    }

    private void connection(String status) {
        FriendConnection c = new FriendConnection();
        c.setStatus(status);
        when(connections.findByUserLowAndUserHigh(user, friend)).thenReturn(Optional.of(c));
    }

    @Test
    void requiresLogin() throws Exception {
        mvc.perform(get("/api/friends/2/feed")).andExpect(status().isUnauthorized());
        verifyNoInteractions(answers, achievements);
    }

    @Test
    void rejectsNonFriendWithoutReadingActivities() throws Exception {
        authenticated();
        mvc.perform(get("/api/friends/2/feed").session(session)).andExpect(status().isNotFound());
        verifyNoInteractions(answers, achievements);
    }

    @Test
    void rejectsPendingOrRejectedFriendships() throws Exception {
        authenticated();
        for (String state : List.of(FriendConnection.PENDING, FriendConnection.REJECTED)) {
            connection(state);
            mvc.perform(get("/api/friends/2/feed").session(session)).andExpect(status().isNotFound());
        }
        verifyNoInteractions(answers, achievements);
    }

    @Test
    void rejectsDeletedFriend() throws Exception {
        authenticated();
        friend.setDeleteFlag(true);
        mvc.perform(get("/api/friends/2/feed").session(session)).andExpect(status().isNotFound());
        verifyNoInteractions(connections, answers, achievements);
    }

    @Test
    void rejectsAdmin() throws Exception {
        user.setPermission(1);
        when(customers.findById(1)).thenReturn(Optional.of(user));
        mvc.perform(get("/api/friends/2/feed").session(session)).andExpect(status().isForbidden());
        verifyNoInteractions(connections, answers, achievements);
    }

    @Test
    void returnsEmptyFeedForFriendWithoutActivities() throws Exception {
        authenticated();
        connection(FriendConnection.ACCEPTED);
        mvc.perform(get("/api/friends/2/feed").session(session))
                .andExpect(status().isOk()).andExpect(content().json("[]"));
    }

    @Test
    void combinesDailyStudyAndAchievementsNewestFirstWithinThirtyDays() throws Exception {
        authenticated();
        connection(FriendConnection.ACCEPTED);
        LocalDate today = LocalDate.now();
        DailyStudySummary day = mock(DailyStudySummary.class);
        when(day.getStudyDate()).thenReturn(today.minusDays(1));
        when(day.getAnswerCount()).thenReturn(10L);
        when(day.getCorrectCount()).thenReturn(8L);
        when(answers.summarizeDailyStudy(friend, today.minusDays(29), today)).thenReturn(List.of(day));
        Achievement a = new Achievement();
        a.setIcon("⭐");
        a.setAchievementName("レベル25到達");
        UserAchievement unlocked = new UserAchievement();
        unlocked.setAchievement(a);
        unlocked.setUnlockedDate(today);
        when(achievements.findByCustomerAndUnlockedDateBetween(friend, today.minusDays(29), today))
                .thenReturn(List.of(unlocked));
        mvc.perform(get("/api/friends/2/feed").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].date").value(today.toString()))
                .andExpect(jsonPath("$[0].type").value("実績"))
                .andExpect(jsonPath("$[1].message").value("10問に解答・8問正解しました。"))
                .andExpect(jsonPath("$[0].customer").doesNotExist());
        verify(achievements).findByCustomerAndUnlockedDateBetween(eq(friend), any(), any());
    }
}
