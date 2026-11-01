package jp.co.sss.pr.validation;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.containsString;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import jp.co.sss.pr.entity.Question;
import jp.co.sss.pr.entity.QuestionReportStatus;
import jp.co.sss.pr.repository.QuestionReportRepository;
import org.springframework.beans.factory.annotation.Autowired;

class AdminLifecycleRuntimeTest extends ValidationSupport {
    @Autowired QuestionReportRepository reports;

    @Test
    void adminQuestionCreateValidateUpdateProtectReferencedAndDeleteLifecycle() throws Exception {
        tab = login(admin);
        long before = questions.count();
        mvc.perform(post("/admin/questions/save").session(browser).param("_tab", tab)
                        .param("questionText", "x"))
                .andExpect(status().isOk()).andExpect(view().name("admin/question_form"))
                .andExpect(model().hasErrors());
        assertEquals(before, questions.count());
        String text = "Lifecycle " + java.util.UUID.randomUUID();
        mvc.perform(questionForm(text)).andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("message", "問題を登録しました。"));
        Question created = questions.findAll().stream()
                .filter(q -> text.equals(q.getQuestionText())).findFirst().orElseThrow();
        assertEquals(2, created.getCorrectOption());
        assertEquals("Explanation", created.getExplanation());
        mvc.perform(get("/admin/questions/" + created.getQuestionId() + "/edit")
                        .session(browser).param("_tab", tab))
                .andExpect(status().isOk()).andExpect(content().string(containsString(text)));
        mvc.perform(questionForm(text + " updated").param("questionId", created.getQuestionId().toString()))
                .andExpect(status().is3xxRedirection());
        assertEquals(text + " updated", questions.findById(created.getQuestionId()).orElseThrow().getQuestionText());

        String userTab = login(user);
        mvc.perform(post("/favorite/toggle").session(browser).param("_tab", userTab)
                        .param("questionId", created.getQuestionId().toString()))
                .andExpect(status().is3xxRedirection());
        mvc.perform(post("/admin/questions/" + created.getQuestionId() + "/delete")
                        .session(browser).param("_tab", tab))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("error",
                        "解答履歴またはブックマークに使われている問題は削除できません。"));
        assertTrue(questions.existsById(created.getQuestionId()));
        mvc.perform(post("/favorite/toggle").session(browser).param("_tab", userTab)
                        .param("questionId", created.getQuestionId().toString()))
                .andExpect(status().is3xxRedirection());
        mvc.perform(post("/admin/questions/" + created.getQuestionId() + "/delete")
                        .session(browser).param("_tab", tab))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("message", "問題を削除しました。"));
        assertFalse(questions.existsById(created.getQuestionId()));
        assertEquals(before, questions.count());
    }

    @Test
    void adminUserUpdateDeleteAndRestorePreserveOtherAccounts() throws Exception {
        tab = login(admin);
        String oldName = user.getUserName();
        String updatedName = friend.getUserName() + "X";
        mvc.perform(post("/user/update/check").session(browser).param("_tab", tab)
                        .param("userId", friend.getUserId().toString())
                        .param("userName", updatedName).param("userPass", "newpass123").param("permission", "0"))
                .andExpect(status().isOk()).andExpect(view().name("user/user_update_check"));
        mvc.perform(post("/user/update/complete").session(browser).param("_tab", tab)
                        .param("userId", friend.getUserId().toString())
                        .param("userName", updatedName).param("permission", "0"))
                .andExpect(status().isOk()).andExpect(view().name("user/user_update_complete"));
        assertTrue(passwords.matches("newpass123", customers.findById(friend.getUserId()).orElseThrow().getUserPass()));
        assertEquals(updatedName, customers.findById(friend.getUserId()).orElseThrow().getUserName());
        mvc.perform(post("/user/delete/check").session(browser).param("_tab", tab)
                        .param("userId", friend.getUserId().toString()).param("deleteFlag", "false"))
                .andExpect(status().isOk()).andExpect(view().name("user/user_delete_check"));
        mvc.perform(post("/user/delete/complete").session(browser).param("_tab", tab)
                        .param("userId", friend.getUserId().toString()))
                .andExpect(status().isOk()).andExpect(view().name("user/user_delete_complete"));
        assertTrue(customers.findById(friend.getUserId()).orElseThrow().getDeleteFlag());
        mvc.perform(post("/login").session(browser).param("_tab", java.util.UUID.randomUUID().toString())
                        .param("userName", updatedName).param("userPass", "newpass123"))
                .andExpect(status().isOk()).andExpect(view().name("login/login"));
        mvc.perform(post("/user/restore/check").session(browser).param("_tab", tab)
                        .param("userId", friend.getUserId().toString()).param("deleteFlag", "true"))
                .andExpect(status().isOk()).andExpect(view().name("user/user_delete_check"));
        mvc.perform(post("/user/delete/complete").session(browser).param("_tab", tab)
                        .param("userId", friend.getUserId().toString()))
                .andExpect(status().isOk());
        assertFalse(customers.findById(friend.getUserId()).orElseThrow().getDeleteFlag());
        assertEquals(oldName, customers.findById(user.getUserId()).orElseThrow().getUserName());
    }

    @Test
    void adminReportInvalidStatusAndMissingIdRemainRecoverableWithoutChangingSavedReport() throws Exception {
        String userTab = login(user);
        mvc.perform(post("/question/" + question.getQuestionId() + "/report")
                        .session(browser).param("_tab", userTab).param("reason", "OTHER").param("detail", "Audit report"))
                .andExpect(status().is3xxRedirection());
        var report = reports.findTop5ByCustomerUserIdAndQuestionQuestionIdOrderByCreatedAtDescReportIdDesc(
                user.getUserId(), question.getQuestionId()).get(0);
        tab = login(admin);
        mvc.perform(post("/admin/question-reports/" + report.getReportId() + "/status")
                        .session(browser).param("_tab", tab).param("status", "INVALID"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("error", "対応状況の指定が正しくありません。"));
        mvc.perform(post("/admin/question-reports/99999999/status")
                        .session(browser).param("_tab", tab).param("status", "RESOLVED"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("error", "対象の報告が見つかりません。"));
        assertEquals(QuestionReportStatus.OPEN, reports.findById(report.getReportId()).orElseThrow().getStatus());
        mvc.perform(post("/admin/question-reports/" + report.getReportId() + "/status")
                        .session(browser).param("_tab", tab).param("status", "RESOLVED"))
                .andExpect(status().is3xxRedirection());
        assertEquals(QuestionReportStatus.RESOLVED, reports.findById(report.getReportId()).orElseThrow().getStatus());
        protectedPage("/admin/question-reports");
    }

    private MockHttpServletRequestBuilder questionForm(String text) {
        return post("/admin/questions/save").session(browser).param("_tab", tab)
                .param("category", category.getCategoryId().toString()).param("difficulty", "1")
                .param("questionText", text).param("optionA", "A").param("optionB", "B").param("optionC", "C")
                .param("correctOption", "2").param("explanation", " Explanation ");
    }
}
