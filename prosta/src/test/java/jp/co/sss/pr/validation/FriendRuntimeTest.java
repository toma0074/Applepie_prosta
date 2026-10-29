package jp.co.sss.pr.validation;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import jp.co.sss.pr.entity.FriendConnection;

class FriendRuntimeTest extends ValidationSupport {
    @Test
    void requestAcceptFeedAndRevokeAccessAfterRemoval() throws Exception {
        mvc.perform(get("/api/friends/" + friend.getUserId() + "/feed").session(browser)
                        .param("_tab", java.util.UUID.randomUUID().toString()))
                .andExpect(status().is3xxRedirection());
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
        mvc.perform(get("/api/friends/" + friend.getUserId() + "/feed").session(browser).param("_tab", tab))
                .andExpect(status().isOk()).andExpect(content().json("[]"));
        protectedPage("/friends");
        mvc.perform(delete("/api/friends/" + friend.getUserId()).session(browser).param("_tab", tab))
                .andExpect(status().isNoContent());
        assertTrue(connections.findById(connection.getFriendConnectionId()).isEmpty());
        // コントローラー単体ではなく、フィルターのトランザクション確定まで含めて404を確認する。
        mvc.perform(get("/api/friends/" + friend.getUserId() + "/feed").session(browser).param("_tab", tab))
                .andExpect(status().isNotFound());
        protectedPage("/friends");
    }

    @Test
    void forbiddenAndMissingFeedKeepTheirHttpStatus() throws Exception {
        tab = login(admin);
        mvc.perform(get("/api/friends/" + friend.getUserId() + "/feed").session(browser).param("_tab", tab))
                .andExpect(status().isForbidden());
        tab = login(user);
        mvc.perform(get("/api/friends/" + friend.getUserId() + "/feed").session(browser).param("_tab", tab))
                .andExpect(status().isNotFound());
        friend.setDeleteFlag(true);
        customers.saveAndFlush(friend);
        mvc.perform(get("/api/friends/" + friend.getUserId() + "/feed").session(browser).param("_tab", tab))
                .andExpect(status().isNotFound());
        protectedPage("/friends");
    }
}
