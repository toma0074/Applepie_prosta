package jp.co.sss.pr.validation;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.HttpMethod;

class PageErrorRuntimeTest extends ValidationSupport {
    @ParameterizedTest(name = "{0} {1} -> {2}")
    @CsvSource({
        "POST, /background/change?background=invalid, 400, USER",
        "POST, /background/change?background=bg-game, 403, USER",
        "POST, /background/changeColor?backgroundColor=invalid, 400, USER",
        "POST, /background/change, 400, USER",
        "GET, /user/update/input?userId={friend}, 403, USER",
        "POST, /user/update/check?userId={friend}, 403, USER",
        "POST, /user/update/complete?userId={friend}, 403, USER",
        "GET, /user/update/input?userId=-1, 404, ADMIN",
        "POST, /user/update/check?userId=-1, 404, ADMIN",
        "POST, /user/update/complete?userId=-1, 404, ADMIN",
        "POST, /user/delete/complete?userId=-1, 404, ADMIN",
        "GET, /admin/questions/-1/edit, 404, ADMIN",
        "POST, /admin/questions/-1/delete, 404, ADMIN",
        "GET, /study-calendar?edit=-1, 404, USER",
        "POST, /study-calendar/plans/-1?planDate=2026-10-30&title=test, 404, USER",
        "POST, /study-calendar/plans/-1/delete, 404, USER",
        "GET, /study-calendar?month=invalid, 400, USER",
        "GET, /mypage/history?page=invalid, 400, USER",
        "POST, /api/chat, 400, USER",
        "POST, /api/chat?emptyMessage=true, 400, USER"
    })
    void expectedErrorsDoNotChangeUserOrBreakNextRequest(
            String method, String path, int expected, String role) throws Exception {
        tab = login("ADMIN".equals(role) ? admin : user);
        var before = customers.findById(user.getUserId()).orElseThrow();
        String name = before.getUserName();
        String background = before.getBackgroundClass();
        long exp = before.getTotalExp();
        path = path.replace("{friend}", friend.getUserId().toString());
        var request = request(HttpMethod.valueOf(method), path).session(browser).param("_tab", tab);
        if (path.contains("emptyMessage")) {
            request.contentType(org.springframework.http.MediaType.APPLICATION_JSON).content("{\"message\":\"\"}");
        }
        mvc.perform(request)
                .andExpect(status().is(expected));
        var after = customers.findById(user.getUserId()).orElseThrow();
        assertEquals(name, after.getUserName());
        assertEquals(background, after.getBackgroundClass());
        assertEquals(exp, after.getTotalExp());
        protectedPage("/mypage");
    }
}
