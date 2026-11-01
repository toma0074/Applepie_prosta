package jp.co.sss.pr.validation;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import jp.co.sss.pr.repository.QuestionNoteRepository;

class AdminErrorRegressionTest extends ValidationSupport {
    @Autowired QuestionNoteRepository notes;
    @Test
    void selfDeletionCannotBypassTheConfirmationGuard() throws Exception {
        tab = login(admin);
        mvc.perform(post("/user/delete/check").session(browser).param("_tab", tab)
                        .param("userId", admin.getUserId().toString()).param("deleteFlag", "false"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("errorMessage", "ログイン中のユーザーは操作できません"));
        mvc.perform(post("/user/delete/complete").session(browser).param("_tab", tab)
                        .param("userId", admin.getUserId().toString()))
                .andExpect(status().isForbidden());
        assertFalse(Boolean.TRUE.equals(customers.findById(admin.getUserId()).orElseThrow().getDeleteFlag()),
                "The completion endpoint must preserve the self-deletion restriction from confirmation");
    }

    @Test
    void deletingUnusedQuestionAlsoDeletesItsMemoAccordingToCascadeSchema() throws Exception {
        String userTab = login(user);
        mvc.perform(post("/question/" + question.getQuestionId() + "/note")
                        .session(browser).param("_tab", userTab).param("content", "Keep this note"))
                .andExpect(status().is3xxRedirection());
        tab = login(admin);
        mvc.perform(post("/admin/questions/" + question.getQuestionId() + "/delete")
                        .session(browser).param("_tab", tab))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("message", "問題を削除しました。"));
        assertFalse(questions.existsById(question.getQuestionId()));
        assertTrue(notes.findByCustomerUserIdAndQuestionQuestionId(
                user.getUserId(), question.getQuestionId()).isEmpty());
        protectedPage("/admin/questions");
    }

    @Test
    void missingDeletionConfirmationTargetReturnsClientErrorRatherThanNullPointerException() throws Exception {
        tab = login(admin);
        mvc.perform(post("/user/delete/check").session(browser).param("_tab", tab))
                .andExpect(status().isBadRequest());
        assertFalse(Boolean.TRUE.equals(customers.findById(admin.getUserId()).orElseThrow().getDeleteFlag()));
        mvc.perform(post("/user/restore/check").session(browser).param("_tab", tab))
                .andExpect(status().isBadRequest());
        protectedPage("/user/list");
    }
}
