package jp.co.sss.pr.validation;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class LearningRuntimeTest extends ValidationSupport {
    @Test
    void gradingHistoryCalendarAndPlanPersistence() throws Exception {
        mvc.perform(post("/study-calendar/plans").session(browser)
                        .param("_tab", java.util.UUID.randomUUID().toString())
                        .param("planDate", LocalDate.now().toString()).param("title", "拒否対象"))
                .andExpect(status().is3xxRedirection());
        tab = login(user);
        mvc.perform(get("/test/category/start").session(browser).param("_tab", tab)
                        .param("category", category.getCategoryId().toString()).param("difficulty", "1"))
                .andExpect(status().isOk());
        mvc.perform(post("/test/check").session(browser).param("_tab", tab)
                        .param("results[0].questionId", question.getQuestionId().toString())
                        .param("results[0].category", category.getCategoryId().toString())
                        .param("results[0].ansOption", "1").param("results[0].correctOption", "1"))
                .andExpect(status().isOk());
        mvc.perform(post("/test/result").session(browser).param("_tab", tab))
                .andExpect(status().is3xxRedirection());
        assertEquals(1, answers.countByCustomerAndCorrectFlagTrue(user));
        protectedPage("/test/result");
        protectedPage("/mypage/history");
        mvc.perform(post("/study-calendar/plans").session(browser).param("_tab", tab)
                        .param("planDate", LocalDate.now().toString()).param("title", "検証予定")
                        .param("details", "検証用の学習予定"))
                .andExpect(status().is3xxRedirection());
        var saved = plans.findAllByCustomerAndPlanDateOrderByStudyPlanIdAsc(user, LocalDate.now());
        assertEquals(1, saved.size());
        protectedPage("/study-calendar");
        mvc.perform(post("/study-calendar/plans/" + saved.get(0).getStudyPlanId() + "/delete")
                        .session(browser).param("_tab", tab))
                .andExpect(status().is3xxRedirection());
        assertTrue(plans.findAllByCustomerAndPlanDateOrderByStudyPlanIdAsc(user, LocalDate.now()).isEmpty());
    }
}
