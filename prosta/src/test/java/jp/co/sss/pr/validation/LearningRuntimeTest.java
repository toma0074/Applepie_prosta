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
        String html = mvc.perform(get("/test/category/start").session(browser).param("_tab", tab)
                        .param("category", category.getCategoryId().toString()).param("difficulty", "1")
                        .param("questionCount", "5")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        var ids = java.util.regex.Pattern.compile("name=\"results\\[(\\d+)\\]\\.questionId\"\\s+value=\"(\\d+)\"")
                .matcher(html);
        var check = post("/test/check").session(browser).param("_tab", tab);
        int count = 0;
        while (ids.find()) {
            check.param("results[" + ids.group(1) + "].questionId", ids.group(2))
                    .param("results[" + ids.group(1) + "].ansOption", "1")
                    .param("results[" + ids.group(1) + "].correctOption", "3");
            count++;
        }
        assertEquals(5, count);
        mvc.perform(check).andExpect(status().isOk());
        mvc.perform(post("/test/result").session(browser).param("_tab", tab))
                .andExpect(status().is3xxRedirection());
        assertEquals(5, answers.countByCustomerAndCorrectFlagTrue(user));
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
