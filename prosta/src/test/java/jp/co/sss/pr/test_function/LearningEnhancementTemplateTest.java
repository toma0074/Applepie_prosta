package jp.co.sss.pr.test_function;

import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.*;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import jp.co.sss.pr.bean.CustomerBean;
import jp.co.sss.pr.entity.*;
import jp.co.sss.pr.repository.*;
import jp.co.sss.pr.service.*;
import jp.co.sss.pr.mypage_function.RecommendationController;

@WebMvcTest({TestController.class, RecommendationController.class})
@AutoConfigureMockMvc(addFilters = false)
@Import({AnswerGradingService.class, StudyReviewService.class})
class LearningEnhancementTemplateTest {
    @Autowired private MockMvc mvc;
    @MockBean private QuestionRepository questions;
    @MockBean private AnswerRepository answers;
    @MockBean private CustomerRepository customers;
    @MockBean private CategoryRepository categories;
    @MockBean private FriendConnectionRepository friends;
    @MockBean private FavoriteRepository favorites;
    @MockBean private LevelService levels;
    @MockBean private WeakQuestionService weak;
    private MockHttpSession loggedIn() {
        CustomerBean bean = new CustomerBean(); bean.setUserId(1); bean.setPermission(0);
        MockHttpSession session = new MockHttpSession(); session.setAttribute("user", bean);
        Customer customer = new Customer(); customer.setUserId(1); customer.setUserLevel(1);
        when(customers.getReferenceById(1)).thenReturn(customer);
        return session;
    }
    private List<Question> questions(int count) {
        List<Question> all = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            Question q = new Question(); q.setQuestionId(i); q.setDifficulty(1);
            q.setCorrectOption(1); q.setQuestionText("Q" + i);
            q.setOptionA("A"); q.setOptionB("B"); q.setOptionC("C");
            all.add(q);
        }
        return all;
    }
    @Test
    void bookmarkSelectionOnlyStartsChosenOwnedQuestionsAndRejectsOthers() throws Exception {
        MockHttpSession session = loggedIn();
        when(favorites.findBookmarkedQuestions(1)).thenReturn(questions(20));
        mvc.perform(get("/test/bookmarks/start").session(session).param("questionCount", "5")
                        .param("questionIds", "2", "4"))
                .andExpect(status().isOk());
        List<?> selected = (List<?>) session.getAttribute("question");
        assertEquals(2, selected.size());
        mvc.perform(get("/test/bookmarks/start").session(session).param("questionIds", "99"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/test/bookmarks/start").session(session).param("questionCount", "100"))
                .andExpect(status().isBadRequest());
    }
    @Test
    void questionCountUsesSharedStylingAndSmallerNoteBelowTheSelector() throws Exception {
        MockHttpSession session = loggedIn();
        mvc.perform(get("/test/category").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("class=\"question-count-select\"")))
                .andExpect(content().string(containsString("aria-describedby=\"question-count-note\"")))
                .andExpect(content().string(containsString(
                        "class=\"question-count-note\">（対象問題が少ない場合はその件数で出題します。）")));
    }

    @Test
    void questionCountFollowsDifficultyAndPrecedesTimerOnBothSelectionPages() throws Exception {
        MockHttpSession session = loggedIn();
        for (String path : List.of("/test/category", "/test/diagnostic")) {
            String html = mvc.perform(get(path).session(session)).andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            int difficulty = html.indexOf("class=\"difficulty\"");
            int count = html.indexOf("class=\"question-count\"");
            int timer = html.indexOf("name=\"timer\"");
            assertTrue(difficulty >= 0 && difficulty < count && count < timer, path);
        }
    }

    @Test
    void categoryCountIsFiveAndCorrectOptionsAreNotSentToBrowser() throws Exception {
        MockHttpSession session = loggedIn();
        when(questions.findAllByCategoryAndDifficulty(any(), eq(1))).thenReturn(questions(20));
        mvc.perform(get("/test/category/start").session(session).param("category", "1")
                        .param("difficulty", "1").param("questionCount", "5"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString(".correctOption\""))));
        assertEquals(5, ((List<?>) session.getAttribute("question")).size());
    }
    @Test
    void tamperedAnswersCannotChangeCorrectnessAndMissingQuestionIsRejected() throws Exception {
        MockHttpSession session = loggedIn();
        List<Question> one = questions(1);
        when(favorites.findBookmarkedQuestions(1)).thenReturn(one);
        when(questions.findAllById(List.of(1))).thenReturn(one);
        mvc.perform(get("/test/bookmarks/start").session(session)).andExpect(status().isOk());
        mvc.perform(post("/test/check").session(session).param("results[0].questionId", "1")
                        .param("results[0].ansOption", "2").param("results[0].correctOption", "2"))
                .andExpect(status().isOk());
        when(levels.processTestResult(any(), eq(0), eq(1))).thenReturn(List.of());
        mvc.perform(post("/test/result").session(session)).andExpect(redirectedUrl("/test/result"));
        verify(answers).save(argThat(a -> Boolean.FALSE.equals(a.getCorrectFlag())));
        mvc.perform(get("/test/result").session(session)).andExpect(model().attribute("ca", 0))
                .andExpect(content().string(containsString("B: B")))
                .andExpect(content().string(containsString("A: A")));
        mvc.perform(get("/test/bookmarks/start").session(session)).andExpect(status().isOk());
        mvc.perform(post("/test/check").session(session).param("results[0].questionId", "99")
                        .param("results[0].ansOption", "1"))
                .andExpect(status().isBadRequest());
    }
    @Test
    void recommendationChoosesDueReviewAndDueQuizShowsExistingQuestion() throws Exception {
        MockHttpSession session = loggedIn();
        Answer previous = new Answer(); previous.setQuestion(questions(1).get(0));
        previous.setAnsDate(LocalDate.now().minusDays(1)); previous.setCorrectFlag(false);
        when(answers.findAllByCustomerOrderByAnsIdDesc(any())).thenReturn(List.of(previous));
        mvc.perform(get("/learning/recommendation").session(session))
                .andExpect(status().isOk()).andExpect(model().attribute("mode", "spaced"));
        mvc.perform(get("/test/spaced").session(session)).andExpect(status().isOk())
                .andExpect(content().string(containsString("今日の復習")));
        mvc.perform(get("/test/spaced/start").session(session).param("questionCount", "5"))
                .andExpect(status().isOk());
        assertEquals(1, ((List<?>) session.getAttribute("question")).size());
    }
    @Test
    void recommendationFallsBackToUnstudiedAvailableCategory() throws Exception {
        MockHttpSession session = loggedIn();
        Category category = new Category(); category.setCategoryId(1); category.setCategoryName("Java");
        Question q = questions(1).get(0); q.setCategory(category);
        when(categories.findAll()).thenReturn(List.of(category));
        when(questions.findAllByOrderByDifficultyAscCategoryAsc()).thenReturn(List.of(q));
        mvc.perform(get("/learning/recommendation").session(session))
                .andExpect(status().isOk()).andExpect(model().attribute("mode", "category"))
                .andExpect(model().attribute("difficulty", 1));
        mvc.perform(get("/learning/recommendation")).andExpect(redirectedUrl("/"));
    }
}
