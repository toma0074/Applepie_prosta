package jp.co.sss.pr.validation;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.containsString;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import jakarta.persistence.EntityManager;

class QuestionStudyRuntimeTest extends ValidationSupport {
    @Autowired EntityManager entityManager;

    @Test
    void invalidStudyInputsShowMessagesWithoutRollbackErrorsOrWrites() throws Exception {
        tab = login(user);
        String path = "/question/" + question.getQuestionId();
        mvc.perform(post(path + "/note").session(browser).param("_tab", tab)
                        .param("content", " \r\n ").param("returnUrl", "/question"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("noteError", "メモを入力してください。"));
        mvc.perform(post(path + "/note").session(browser).param("_tab", tab)
                        .param("content", "a".repeat(1001)))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("noteError", "メモは1000文字以内で入力してください。"));
        assertEquals(0L, count("question_note"));
        mvc.perform(post(path + "/report").session(browser).param("_tab", tab)
                        .param("reason", "OTHER").param("detail", " "))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("reportError", "「その他」を選んだ場合は報告内容を入力してください。"));
        assertEquals(0L, count("question_report"));
        mvc.perform(post(path + "/note").session(browser).param("_tab", tab)
                        .param("content", "valid memo"))
                .andExpect(status().is3xxRedirection());
        mvc.perform(post(path + "/note").session(browser).param("_tab", tab)
                        .param("content", ""))
                .andExpect(flash().attribute("noteError", "メモを入力してください。"));
        mvc.perform(get(path + "/study").session(browser).param("_tab", tab))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("valid memo")));
        assertEquals(1L, count("question_note"));
        mvc.perform(post(path + "/report").session(browser).param("_tab", tab)
                        .param("reason", "UNCLEAR_EXPLANATION"))
                .andExpect(status().is3xxRedirection());
        mvc.perform(post(path + "/report").session(browser).param("_tab", tab)
                        .param("reason", "UNCLEAR_EXPLANATION"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("reportError", "同じ理由の報告はすでに受け付けています。対応をお待ちください。"));
        assertEquals(1L, count("question_report"));
    }

    @Test
    void listNavigationDoesNotToggleAndStudyButtonAddsAndRemovesBookmark() throws Exception {
        tab = login(user);
        String path = "/question/" + question.getQuestionId() + "/study";
        mvc.perform(get("/question").session(browser).param("_tab", tab))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("action=\"" + path + "?_tab=" + tab + "\" method=\"get\"")));
        mvc.perform(get(path).session(browser).param("_tab", tab).param("returnUrl", "/question"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("bookmarked", false))
                .andExpect(content().string(containsString("☆ 登録する")));
        assertEquals(0L, count("favorite"));
        mvc.perform(post("/favorite/toggle").session(browser).param("_tab", tab)
                        .param("questionId", question.getQuestionId().toString())
                        .param("studyPage", "true").param("returnUrl", "/question"))
                .andExpect(redirectedUrl(path + "?returnUrl=/question&_tab=" + tab));
        assertEquals(1L, count("favorite"));
        mvc.perform(get("/question/bookmarks").session(browser).param("_tab", tab))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("name=\"returnUrl\" value=\"/question/bookmarks\"")));
        mvc.perform(get(path).session(browser).param("_tab", tab).param("returnUrl", "/question/bookmarks"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("bookmarked", true))
                .andExpect(content().string(containsString("★ 登録解除")));
        assertEquals(1L, count("favorite"));
        mvc.perform(post("/favorite/toggle").session(browser).param("_tab", tab)
                        .param("questionId", question.getQuestionId().toString())
                        .param("studyPage", "true").param("returnUrl", "/question/bookmarks"))
                .andExpect(redirectedUrl(path + "?returnUrl=/question/bookmarks&_tab=" + tab));
        assertEquals(0L, count("favorite"));
        mvc.perform(get(path).session(browser).param("_tab", tab).param("returnUrl", "/question/bookmarks"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("bookmarked", false))
                .andExpect(content().string(containsString("ブックマーク一覧に戻る")));
    }

    @Test
    void privateMemoPersistsAndReportsReachOnlyAdministrators() throws Exception {
        String path = "/question/" + question.getQuestionId();
        mvc.perform(post(path + "/note").session(browser).param("_tab", java.util.UUID.randomUUID().toString())
                        .param("content", "拒否されるメモ"))
                .andExpect(status().is3xxRedirection());
        tab = login(user);
        mvc.perform(post(path + "/note").session(browser).param("_tab", tab)
                        .param("content", "整数型を復習する").param("returnUrl", "/test/result"))
                .andExpect(status().is3xxRedirection());
        mvc.perform(get(path + "/study").session(browser).param("_tab", tab))
                .andExpect(status().isOk()).andExpect(content().string(containsString("整数型を復習する")));
        assertEquals(1L, count("question_note"));
        String friendTab = login(friend);
        mvc.perform(get(path + "/study").session(browser).param("_tab", friendTab))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("整数型を復習する"))));
        mvc.perform(post(path + "/report").session(browser).param("_tab", tab)
                        .param("reason", "UNCLEAR_EXPLANATION").param("detail", "具体例を追加してください"))
                .andExpect(status().is3xxRedirection());
        assertEquals(1L, count("question_report"));
        mvc.perform(get("/admin/question-reports").session(browser).param("_tab", tab))
                .andExpect(status().isForbidden());
        String adminTab = login(admin);
        mvc.perform(get("/admin/question-reports").session(browser).param("_tab", adminTab))
                .andExpect(status().isOk()).andExpect(content().string(containsString("具体例を追加してください")));
        Number reportId = (Number) entityManager.createNativeQuery(
                "select report_id from question_report where user_id = :user")
                .setParameter("user", user.getUserId()).getSingleResult();
        mvc.perform(post("/admin/question-reports/" + reportId.longValue() + "/status")
                        .session(browser).param("_tab", adminTab).param("status", "RESOLVED").param("filter", "OPEN"))
                .andExpect(status().is3xxRedirection());
        assertEquals("RESOLVED", entityManager.createNativeQuery(
                "select status from question_report where report_id = :id")
                .setParameter("id", reportId).getSingleResult());
        mvc.perform(post(path + "/note/delete").session(browser).param("_tab", tab))
                .andExpect(status().is3xxRedirection());
        assertEquals(0L, count("question_note"));
    }

    private long count(String table) {
        return ((Number) entityManager.createNativeQuery(
                "select count(*) from " + table + " where user_id = :user")
                .setParameter("user", user.getUserId()).getSingleResult()).longValue();
    }
}
