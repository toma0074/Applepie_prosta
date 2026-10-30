package jp.co.sss.pr.validation;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import jp.co.sss.pr.entity.Answer;
import jp.co.sss.pr.entity.FriendConnection;

class FriendAccountDeletionRuntimeTest extends ValidationSupport {
    @Test
    void deletedFriendDisappearsAndRestorationPreservesRelationshipAndHistory() throws Exception {
        mvc.perform(get("/api/friends").session(browser).param("_tab", UUID.randomUUID().toString()))
                .andExpect(status().is3xxRedirection());
        acceptFriendRequest();
        Answer answer = new Answer();
        answer.setCustomer(friend);
        answer.setQuestion(question);
        answer.setAnsOption(1);
        answer.setCorrectOption(1);
        answer.setCorrectFlag(true);
        answer.setAnsDate(LocalDate.now());
        answer.setAnsCount(1);
        answer = answers.saveAndFlush(answer);

        assertFriendVisible();
        mvc.perform(get(feedPath()).session(browser).param("_tab", tab))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        String adminTab = login(admin);
        toggleFriendAccount(adminTab);
        assertTrue(customers.findById(friend.getUserId()).orElseThrow().getDeleteFlag());
        assertFriendHidden();
        assertEquals(FriendConnection.ACCEPTED,
                connections.findByUserLowAndUserHigh(user, friend).orElseThrow().getStatus());
        assertTrue(answers.findById(answer.getAnsId()).isPresent());
        protectedPage("/friends");

        toggleFriendAccount(adminTab);
        assertFalse(customers.findById(friend.getUserId()).orElseThrow().getDeleteFlag());
        assertFriendVisible();
        mvc.perform(get(feedPath()).session(browser).param("_tab", tab))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void deletionRevokesExistingFriendLoginAndRejectsNewLogin() throws Exception {
        String friendTab = acceptFriendRequest();
        String adminTab = login(admin);
        toggleFriendAccount(adminTab);
        mvc.perform(get("/api/friends/me").session(browser).param("_tab", friendTab))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("X-Prosta-Login-Expired", "true"));
        mvc.perform(post("/login").session(browser).param("_tab", UUID.randomUUID().toString())
                        .param("userName", friend.getUserName()).param("userPass", "pass1234"))
                .andExpect(status().isOk()).andExpect(view().name("login/login"))
                .andExpect(model().attributeExists("errMessage"));
        assertFriendHidden();
        protectedPage("/friends");

        toggleFriendAccount(adminTab);
        mvc.perform(get("/api/friends/me").session(browser).param("_tab", friendTab))
                .andExpect(status().is3xxRedirection());
        String newFriendTab = login(customers.findById(friend.getUserId()).orElseThrow());
        mvc.perform(get("/api/friends/me").session(browser).param("_tab", newFriendTab))
                .andExpect(status().isOk()).andExpect(jsonPath("$.userId").value(friend.getUserId()));
    }

    @Test
    void removingDeletedFriendPreventsRelationshipReturningAfterRestoration() throws Exception {
        acceptFriendRequest();
        String adminTab = login(admin);
        toggleFriendAccount(adminTab);
        assertFriendHidden();
        mvc.perform(delete("/api/friends/" + friend.getUserId()).session(browser).param("_tab", tab))
                .andExpect(status().isNoContent());
        assertTrue(connections.findByUserLowAndUserHigh(user, friend).isEmpty());

        toggleFriendAccount(adminTab);
        mvc.perform(get("/api/friends").session(browser).param("_tab", tab))
                .andExpect(status().isOk()).andExpect(content().json("[]"));
        mvc.perform(get(feedPath()).session(browser).param("_tab", tab))
                .andExpect(status().isNotFound());
        assertTrue(connections.findByUserLowAndUserHigh(user, friend).isEmpty());
        protectedPage("/friends");
    }

    private String acceptFriendRequest() throws Exception {
        tab = login(user);
        mvc.perform(post("/api/friends/requests/" + friend.getUserId()).session(browser).param("_tab", tab))
                .andExpect(status().isCreated());
        var connection = connections.findByUserLowAndUserHigh(user, friend).orElseThrow();
        assertEquals(FriendConnection.PENDING, connection.getStatus());
        String friendTab = login(friend);
        mvc.perform(post("/api/friends/requests/" + connection.getFriendConnectionId() + "/accept")
                        .session(browser).param("_tab", friendTab))
                .andExpect(status().isNoContent());
        assertEquals(FriendConnection.ACCEPTED,
                connections.findById(connection.getFriendConnectionId()).orElseThrow().getStatus());
        return friendTab;
    }

    private void toggleFriendAccount(String adminTab) throws Exception {
        mvc.perform(post("/user/delete/complete").session(browser).param("_tab", adminTab)
                        .param("userId", friend.getUserId().toString()))
                .andExpect(status().isOk()).andExpect(view().name("user/user_delete_complete"));
    }

    private void assertFriendVisible() throws Exception {
        mvc.perform(get("/api/friends").session(browser).param("_tab", tab))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].userId").value(friend.getUserId()));
        mvc.perform(get("/api/friends/leaderboard").session(browser).param("_tab", tab))
                .andExpect(status().isOk()).andExpect(jsonPath("$.entries.length()").value(2));
    }

    private void assertFriendHidden() throws Exception {
        mvc.perform(get("/api/friends").session(browser).param("_tab", tab))
                .andExpect(status().isOk()).andExpect(content().json("[]"));
        mvc.perform(get("/api/friends/leaderboard").session(browser).param("_tab", tab))
                .andExpect(status().isOk()).andExpect(jsonPath("$.entries.length()").value(1))
                .andExpect(jsonPath("$.entries[0].userId").value(user.getUserId()));
        mvc.perform(get("/api/friends/users").session(browser).param("_tab", tab)
                        .param("query", friend.getUserName()))
                .andExpect(status().isOk()).andExpect(content().json("[]"));
        mvc.perform(get(feedPath()).session(browser).param("_tab", tab))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/friends/requests/" + friend.getUserId()).session(browser).param("_tab", tab))
                .andExpect(status().isNotFound());
    }

    private String feedPath() {
        return "/api/friends/" + friend.getUserId() + "/feed";
    }
}
