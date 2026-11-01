package jp.co.sss.pr.validation;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.containsString;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import jp.co.sss.pr.repository.FavoriteRepository;

class BookmarkReviewRuntimeTest extends ValidationSupport {
    @Autowired FavoriteRepository favorites;

    @Test
    void selectedBookmarksGradeWithDatabaseAnswersAndKeepQuestCompletion() throws Exception {
        mvc.perform(get("/question/bookmarks").session(browser).param("_tab", java.util.UUID.randomUUID().toString()))
                .andExpect(status().is3xxRedirection());
        tab = login(user);
        long originalExp = user.getTotalExp();
        String html = mvc.perform(get("/test/category/start").session(browser).param("_tab", tab)
                        .param("category", category.getCategoryId().toString()).param("difficulty", "1")
                        .param("questionCount", "5")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertEquals(5, submitRenderedQuiz(html, 1));
        var updated = customers.findById(user.getUserId()).orElseThrow();
        assertEquals(originalExp + 25, updated.getTotalExp());
        assertEquals(LocalDate.now(), updated.getQuestRewardDate());
        assertEquals(7, updated.getQuestCompletedMask());
        var bookmarked = answers.findAllByCustomerOrderByAnsIdDesc(user).get(0).getQuestion();
        mvc.perform(get("/test/result").session(browser).param("_tab", tab))
                .andExpect(status().isOk()).andExpect(content().string(containsString("10 EXP")));
        mvc.perform(post("/favorite/toggle").session(browser).param("_tab", tab)
                        .param("questionId", bookmarked.getQuestionId().toString()).param("resultPage", "true"))
                .andExpect(redirectedUrl("/test/result?_tab=" + tab));
        assertTrue(favorites.existsByCustomerAndQuestion(user, bookmarked));
        mvc.perform(get("/question/bookmarks").session(browser).param("_tab", tab))
                .andExpect(status().isOk()).andExpect(content().string(containsString(bookmarked.getQuestionText())));
        html = mvc.perform(get("/test/bookmarks/start").session(browser).param("_tab", tab)
                        .param("questionIds", bookmarked.getQuestionId().toString()).param("questionCount", "5"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertEquals(1, submitRenderedQuiz(html, 2));
        updated = customers.findById(user.getUserId()).orElseThrow();
        assertEquals(originalExp + 25, updated.getTotalExp());
        assertEquals(7, updated.getQuestCompletedMask());
        mvc.perform(get("/test/result").session(browser).param("_tab", tab))
                .andExpect(status().isOk()).andExpect(content().string(containsString("前回：")));
        mvc.perform(post("/favorite/toggle").session(browser).param("_tab", tab)
                        .param("questionId", bookmarked.getQuestionId().toString()).param("bookmarkList", "true"))
                .andExpect(status().is3xxRedirection());
        assertFalse(favorites.existsByCustomerAndQuestion(user, bookmarked));
    }

    @Test
    void dueReviewRecommendationAndConfiguredPlanStartTheIntendedQuestions() throws Exception {
        tab = login(user);
        var answer = new jp.co.sss.pr.entity.Answer();
        answer.setCustomer(user); answer.setQuestion(question); answer.setAnsDate(LocalDate.now().minusDays(1));
        answer.setAnsOption(2); answer.setCorrectOption(1); answer.setCorrectFlag(false); answer.setAnsCount(1);
        answers.saveAndFlush(answer);
        mvc.perform(get("/learning/recommendation").session(browser).param("_tab", tab))
                .andExpect(status().isOk()).andExpect(model().attribute("mode", "spaced"));
        String html = mvc.perform(get("/test/spaced/start").session(browser).param("_tab", tab)
                        .param("questionCount", "5")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertEquals(1, submitRenderedQuiz(html, 1));
        mvc.perform(get("/test/spaced").session(browser).param("_tab", tab))
                .andExpect(status().isOk()).andExpect(content().string(containsString("復習")));
        mvc.perform(post("/study-calendar/plans").session(browser).param("_tab", tab)
                        .param("planDate", LocalDate.now().toString()).param("title", "5問学習")
                        .param("categoryId", category.getCategoryId().toString()).param("targetQuestionCount", "5"))
                .andExpect(status().is3xxRedirection());
        var saved = plans.findAllByCustomerAndPlanDateOrderByStudyPlanIdAsc(user, LocalDate.now());
        assertEquals(1, saved.size());
        mvc.perform(get("/study-calendar").session(browser).param("_tab", tab))
                .andExpect(status().isOk()).andExpect(content().string(containsString("questionCount=5")));
        mvc.perform(post("/study-calendar/plans/" + saved.get(0).getStudyPlanId() + "/delete")
                        .session(browser).param("_tab", tab)).andExpect(status().is3xxRedirection());
        assertTrue(plans.findAllByCustomerAndPlanDateOrderByStudyPlanIdAsc(user, LocalDate.now()).isEmpty());
    }
}
