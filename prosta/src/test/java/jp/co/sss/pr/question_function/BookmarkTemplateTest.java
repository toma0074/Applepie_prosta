package jp.co.sss.pr.question_function;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;
import java.util.Optional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.util.ReflectionTestUtils;

import jp.co.sss.pr.bean.CustomerBean;
import jp.co.sss.pr.entity.Customer;
import jp.co.sss.pr.entity.Question;
import jp.co.sss.pr.repository.*;
import jp.co.sss.pr.service.LevelService;
import jp.co.sss.pr.service.WeakQuestionService;
import jp.co.sss.pr.test_function.TestController;

@WebMvcTest({QuestionController.class, TestController.class})
@AutoConfigureMockMvc(addFilters = false)
@org.springframework.context.annotation.Import(jp.co.sss.pr.service.AnswerGradingService.class)
class BookmarkTemplateTest {
    @Autowired
    private MockMvc mvc;
    @MockBean private QuestionRepository questions;
    @MockBean private FavoriteRepository favorites;
    @MockBean private CustomerRepository customers;
    @MockBean private CategoryRepository categories;
    @MockBean private FriendConnectionRepository friends;
    @MockBean private AnswerRepository answers;
    @MockBean private LevelService levels;
    @MockBean private WeakQuestionService weak;
    @MockBean private EntityManager entities;
    @MockBean private jp.co.sss.pr.service.StudyReviewService studyReviewService;

    private MockMvc toggleMvc(MockHttpSession session) {
        FavoriteController controller = new FavoriteController();
        ReflectionTestUtils.setField(controller, "favoriteRepository", favorites);
        ReflectionTestUtils.setField(controller, "questionRepository", questions);
        ReflectionTestUtils.setField(controller, "entityManager", entities);
        ReflectionTestUtils.setField(controller, "session", session);
        return MockMvcBuilders.standaloneSetup(controller).build();
    }

    private MockHttpSession loggedIn() {
        CustomerBean user = new CustomerBean();
        user.setUserId(7);
        user.setUserName("test");
        user.setPermission(0);
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("user", user);
        return session;
    }

    private Question question() {
        Question q = new Question();
        q.setQuestionId(42);
        q.setQuestionText("Bookmarked question");
        q.setDifficulty(1);
        q.setOptionA("A");
        q.setOptionB("B");
        q.setOptionC("C");
        q.setCorrectOption(1);
        q.setExplanation("Explanation");
        return q;
    }

    @Test
    void listShowsStarsAndStartButtonAndLinksWithContextPath() throws Exception {
        when(favorites.findBookmarkedQuestions(7)).thenReturn(List.of(question()));
        mvc.perform(get("/prosta/question/bookmarks").contextPath("/prosta").session(loggedIn()))
                .andExpect(status().isOk())
                .andExpect(model().attribute("favoriteIds", java.util.Set.of(42)))
                .andExpect(content().string(containsString("Bookmarked question")))
                .andExpect(content().string(containsString("value=\"★\"")))
                .andExpect(content().string(containsString("name=\"returnUrl\" value=\"/question/bookmarks\"")))
                .andExpect(content().string(containsString("action=\"/prosta/test/bookmarks/start\"")))
                .andExpect(content().string(containsString("class=\"bookmark-filters\"")))
                .andExpect(content().string(containsString("class=\"category\" name=\"category\"")))
                .andExpect(content().string(containsString("class=\"difficulty\" name=\"difficulty\"")))
                .andExpect(content().string(containsString("class=\"question-selection\">出題対象")))
                .andExpect(content().string(containsString("class=\"btn bookmark-start-button\"")))
                .andExpect(content().string(containsString("action=\"/prosta/question/42/study\" method=\"get\"")))
                .andExpect(content().string(not(containsString("action=\"/prosta/favorite/toggle\""))))
                .andExpect(content().string(not(containsString(">解説・学習メモ・報告</a>"))))
                .andExpect(content().string(containsString("href=\"/prosta/question/bookmarks\"")));
    }

    @Test
    void studyBookmarkButtonsToggleAndKeepTheOriginalListReturnUrl() throws Exception {
        Customer me = new Customer();
        Question q = question();
        when(entities.find(Customer.class, 7, LockModeType.PESSIMISTIC_WRITE)).thenReturn(me);
        when(questions.findById(42)).thenReturn(Optional.of(q));
        MockHttpSession session = loggedIn();
        MockMvc toggle = toggleMvc(session);
        toggle.perform(post("/favorite/toggle").session(session).param("questionId", "42")
                        .param("studyPage", "true"))
                .andExpect(redirectedUrl("/question/42/study?returnUrl=/question"));
        verify(favorites).save(any());
        when(favorites.existsByCustomerAndQuestion(me, q)).thenReturn(true);
        toggle.perform(post("/favorite/toggle").session(session).param("questionId", "42")
                        .param("studyPage", "true").param("returnUrl", "/question/bookmarks"))
                .andExpect(redirectedUrl("/question/42/study?returnUrl=/question/bookmarks"));
        verify(favorites).deleteByCustomerAndQuestion(me, q);
        toggle.perform(post("/favorite/toggle").session(session).param("questionId", "42")
                        .param("studyPage", "true").param("returnUrl", "https://example.com"))
                .andExpect(redirectedUrl("/question/42/study?returnUrl=/question"));
        toggle.perform(post("/favorite/toggle").session(session).param("questionId", "99")
                        .param("studyPage", "true").param("returnUrl", "/question/bookmarks"))
                .andExpect(redirectedUrl("/question/bookmarks"))
                .andExpect(flash().attribute("error", "対象の問題が見つかりません。"));
    }

    @Test
    void emptyListExplainsHowToRegisterAndDoesNotOfferStart() throws Exception {
        mvc.perform(get("/question/bookmarks").session(loggedIn()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("ブックマークした問題はありません。")))
                .andExpect(content().string(not(containsString("ブックマークから出題"))));
    }

    @Test
    void anonymousCannotListOrStart() throws Exception {
        mvc.perform(get("/question/bookmarks"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/"));
        mvc.perform(get("/test/bookmarks/start"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/"));
        verifyNoInteractions(favorites);
    }

    @Test
    void removeReturnsToBookmarkListAndMissingQuestionDisplaysAnError() throws Exception {
        Customer me = new Customer();
        me.setUserId(7);
        Question q = question();
        when(entities.find(Customer.class, 7, LockModeType.PESSIMISTIC_WRITE)).thenReturn(me);
        when(questions.findById(42)).thenReturn(Optional.of(q));
        when(favorites.existsByCustomerAndQuestion(me, q)).thenReturn(true);
        MockHttpSession session = loggedIn();
        MockMvc toggle = toggleMvc(session);
        toggle.perform(post("/favorite/toggle").session(session)
                        .param("questionId", "42").param("bookmarkList", "true"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/question/bookmarks"));
        verify(favorites).deleteByCustomerAndQuestion(me, q);
        mvc.perform(get("/question/bookmarks").session(session))
                .andExpect(content().string(containsString("ブックマークした問題はありません。")));

        toggle.perform(post("/favorite/toggle").session(session)
                        .param("questionId", "99").param("bookmarkList", "true"))
                .andExpect(redirectedUrl("/question/bookmarks"))
                .andExpect(flash().attribute("error", "対象の問題が見つかりません。"));
    }

    @Test
    void toggleFromSearchStillReturnsToSearch() throws Exception {
        Customer me = new Customer();
        Question q = question();
        when(entities.find(Customer.class, 7, LockModeType.PESSIMISTIC_WRITE)).thenReturn(me);
        when(questions.findById(42)).thenReturn(Optional.of(q));
        MockHttpSession session = loggedIn();
        toggleMvc(session).perform(post("/favorite/toggle").session(session).param("questionId", "42"))
                .andExpect(redirectedUrl("/question"));
        verify(favorites).save(any());
    }

    @Test
    void bookmarkTestUsesExistingConfirmationAndResultFlow() throws Exception {
        Question q = question();
        when(favorites.findBookmarkedQuestions(7)).thenReturn(List.of(q));
        Customer me = new Customer();
        me.setUserId(7);
        me.setUserLevel(1);
        when(customers.getReferenceById(7)).thenReturn(me);
        when(levels.processTestResult(me, 1, 1)).thenReturn(List.of());
        when(questions.findAllById(any())).thenReturn(List.of(q));
        MockHttpSession session = loggedIn();
        mvc.perform(get("/test/bookmarks/start").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Bookmarked question")))
                .andExpect(content().string(not(containsString("id=\"test-timer\""))));
        mvc.perform(post("/test/check").session(session)
                        .param("results[0].questionId", "42")
                        .param("results[0].correctOption", "1")
                        .param("results[0].ansOption", "1")
                        .param("results[0].questionText", "Bookmarked question"))
                .andExpect(status().isOk()).andExpect(view().name("test/review_test"));
        mvc.perform(post("/test/result").session(session))
                .andExpect(redirectedUrl("/test/result"));
        verify(answers).save(org.mockito.ArgumentMatchers.argThat(answer ->
                answer.getCustomer() == me && answer.getQuestion().getQuestionId() == 42
                        && Boolean.TRUE.equals(answer.getCorrectFlag())));
        verify(levels).processTestResult(eq(me), eq(1), eq(1));
        assertNull(session.getAttribute("question"));
        mvc.perform(get("/test/result").session(session))
                .andExpect(status().isOk())
                .andExpect(model().attribute("qc", 1))
                .andExpect(model().attribute("ca", 1));
        mvc.perform(post("/test/result").session(session)).andExpect(redirectedUrl("/mypage"));
        verify(answers, times(1)).save(any());
        assertEquals(1, me.getUserLevel());
    }
}
