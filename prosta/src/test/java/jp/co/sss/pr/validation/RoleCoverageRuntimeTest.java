package jp.co.sss.pr.validation;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.HttpMethod;

class RoleCoverageRuntimeTest extends ValidationSupport {
    @ParameterizedTest(name = "anonymous/user/admin: {0}")
    @ValueSource(strings = {
            "/mypage", "/mypage/history", "/achievement", "/background", "/learning-status",
            "/study-calendar", "/user/update/input", "/question", "/question/bookmarks",
            "/test/category", "/test/diagnostic", "/test/weak", "/test/review", "/test/spaced",
            "/learning/recommendation"
    })
    void sharedPagesRejectAnonymousThenRenderForBothRolesWithoutLearningWrites(String path) throws Exception {
        mvc.perform(get(path).session(browser).param("_tab", java.util.UUID.randomUUID().toString()))
                .andExpect(status().is3xxRedirection());
        long answersBefore = answers.count();
        long plansBefore = plans.count();
        for (var actor : java.util.List.of(user, admin)) {
            tab = login(actor);
            mvc.perform(get(path).session(browser).param("_tab", tab))
                    .andExpect(status().isOk())
                    .andExpect(content().string(org.hamcrest.Matchers.containsString("<html")));
            assertEquals(answersBefore, answers.count());
            assertEquals(plansBefore, plans.count());
        }
    }

    @ParameterizedTest(name = "admin boundary: {0} {1}")
    @CsvSource({
            "GET, /admin/menu",
            "GET, /admin/questions",
            "GET, /admin/questions/new",
            "GET, /admin/questions/{question}/edit",
            "GET, /admin/question-reports",
            "GET, /user/list",
            "POST, /admin/questions/{question}/delete",
            "POST, /admin/questions/save",
            "POST, /admin/question-reports/1/status",
            "POST, /user/delete/check",
            "POST, /user/restore/check",
            "POST, /user/delete/complete"
    })
    void adminRoutesRejectAnonymousAndOrdinaryUsersWithoutWrites(String method, String path) throws Exception {
        path = path.replace("{question}", question.getQuestionId().toString());
        long questionsBefore = questions.count();
        mvc.perform(request(HttpMethod.valueOf(method), path).session(browser)
                        .param("_tab", java.util.UUID.randomUUID().toString())
                        .param("userId", friend.getUserId().toString()))
                .andExpect(status().is3xxRedirection());
        tab = login(user);
        mvc.perform(request(HttpMethod.valueOf(method), path).session(browser).param("_tab", tab)
                        .param("userId", friend.getUserId().toString()))
                .andExpect(status().isForbidden());
        assertEquals(questionsBefore, questions.count());
        assertFalse(Boolean.TRUE.equals(customers.findById(friend.getUserId()).orElseThrow().getDeleteFlag()));
        protectedPage("/mypage");
        if ("GET".equals(method)) {
            tab = login(admin);
            protectedPage(path);
        }
    }
}
