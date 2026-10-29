package jp.co.sss.pr.validation;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.HttpMethod;
import jp.co.sss.pr.entity.FriendConnection;

class FriendErrorRuntimeTest extends ValidationSupport {
    @ParameterizedTest(name = "{0} {1} -> {2} ({3})")
    @CsvSource({
        "GET, /api/friends/users?query=, 400, NONE",
        "GET, /api/friends/leaderboard?difficulty=4, 400, NONE",
        "GET, /api/friends/leaderboard?categoryId=-1, 400, NONE",
        "GET, /api/friends/leaderboard?from=2026-02-02&to=2026-02-01, 400, NONE",
        "GET, /api/friends/leaderboard?from=2020-01-01&to=2026-01-01, 400, NONE",
        "GET, /api/friends/leaderboard?from=2999-01-01&to=2999-01-01, 400, NONE",
        "GET, /api/friends/leaderboard?difficulty=bad, 400, NONE",
        "POST, /api/friends/requests/{self}, 400, NONE",
        "POST, /api/friends/requests/-1, 404, NONE",
        "POST, /api/friends/requests/{friend}, 404, DELETED",
        "POST, /api/friends/requests/{admin}, 404, NONE",
        "POST, /api/friends/requests/{friend}, 409, PENDING",
        "POST, /api/friends/requests/{friend}, 409, ACCEPTED",
        "POST, /api/friends/requests/{id}/accept, 403, PENDING",
        "POST, /api/friends/requests/{id}/reject, 403, PENDING",
        "DELETE, /api/friends/requests/{id}, 403, INCOMING",
        "POST, /api/friends/requests/-1/accept, 404, NONE",
        "POST, /api/friends/requests/-1/reject, 404, NONE",
        "DELETE, /api/friends/requests/-1, 404, NONE",
        "POST, /api/friends/requests/{id}/accept, 409, ACCEPTED",
        "POST, /api/friends/requests/{id}/reject, 409, ACCEPTED",
        "DELETE, /api/friends/requests/{id}, 409, ACCEPTED",
        "POST, /api/friends/requests/{id}/accept, 404, UNRELATED",
        "DELETE, /api/friends/-1, 404, NONE",
        "DELETE, /api/friends/{friend}, 404, NONE",
        "DELETE, /api/friends/{friend}, 404, PENDING",
        "GET, /api/friends, 403, ADMIN",
        "GET, /api/friends/me, 403, ADMIN",
        "GET, /api/friends/requests, 403, ADMIN",
        "GET, /api/friends/users?query=U, 403, ADMIN",
        "GET, /api/friends/leaderboard, 403, ADMIN",
        "POST, /api/friends/requests/{friend}, 403, ADMIN"
    })
    void expectedErrorsPreserveStatusAndStoredRelationship(
            String method, String path, int expected, String state) throws Exception {
        tab = login("ADMIN".equals(state) ? admin : user);
        Integer connectionId = null;
        if ("DELETED".equals(state)) {
            friend.setDeleteFlag(true);
            customers.saveAndFlush(friend);
        } else if (java.util.Set.of("PENDING", "INCOMING", "ACCEPTED", "UNRELATED").contains(state)) {
            acceptedFriendship();
            var connection = connections.findByUserLowAndUserHigh(user, friend).orElseThrow();
            connectionId = connection.getFriendConnectionId();
            if (!"ACCEPTED".equals(state)) connection.setStatus(FriendConnection.PENDING);
            if ("INCOMING".equals(state)) connection.setRequestedBy(friend);
            connections.saveAndFlush(connection);
            if ("UNRELATED".equals(state)) tab = login(customer("X" + user.getUserName().substring(1), 0, 1));
        }
        String oldStatus = connectionId == null ? null : connections.findById(connectionId).orElseThrow().getStatus();
        long count = connections.count();
        path = path.replace("{self}", user.getUserId().toString())
                .replace("{friend}", friend.getUserId().toString())
                .replace("{admin}", admin.getUserId().toString())
                .replace("{id}", String.valueOf(connectionId));
        mvc.perform(request(HttpMethod.valueOf(method), path).session(browser).param("_tab", tab))
                .andExpect(status().is(expected));
        assertEquals(count, connections.count());
        if (connectionId != null) {
            assertEquals(oldStatus, connections.findById(connectionId).orElseThrow().getStatus());
        }
        protectedPage("/mypage");
    }
}
