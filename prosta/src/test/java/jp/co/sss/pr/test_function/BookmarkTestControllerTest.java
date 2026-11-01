package jp.co.sss.pr.test_function;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.ui.ExtendedModelMap;

import jp.co.sss.pr.bean.CustomerBean;
import jp.co.sss.pr.entity.Question;
import jp.co.sss.pr.form.AnswerListForm;
import jp.co.sss.pr.repository.FavoriteRepository;

class BookmarkTestControllerTest {
    private TestController controller;
    private MockHttpSession session;
    private FavoriteRepository favorites;

    @BeforeEach
    void setUp() {
        controller = new TestController();
        session = new MockHttpSession();
        favorites = mock(FavoriteRepository.class);
        controller.session = session;
        controller.favoriteRepository = favorites;
        CustomerBean user = new CustomerBean();
        user.setUserId(7);
        session.setAttribute("user", user);
    }

    private List<Question> questions(int count) {
        List<Question> questions = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Question q = new Question();
            q.setQuestionId(i + 1);
            questions.add(q);
        }
        return List.copyOf(questions);
    }

    @Test
    void picksFifteenDistinctBookmarkedQuestionsWithoutChangingTheSource() {
        List<Question> source = questions(22);
        when(favorites.findBookmarkedQuestions(7)).thenReturn(source);
        ExtendedModelMap model = new ExtendedModelMap();

        assertEquals("test/show_test", controller.startBookmarks(model));
        List<?> selected = (List<?>) session.getAttribute("question");
        assertEquals(15, selected.size());
        assertEquals(15, selected.stream().distinct().count());
        assertTrue(source.containsAll(selected));
        assertEquals(15, ((List<?>) session.getAttribute("ans")).size());
        assertInstanceOf(AnswerListForm.class, model.get("answerListForm"));
        assertEquals(22, source.size());
        verify(favorites).findBookmarkedQuestions(7);
    }

    @Test
    void fewerThanFifteenUsesAllAndClearsPreviousTestFlagsAndTimer() {
        List<Question> source = questions(3);
        when(favorites.findBookmarkedQuestions(7)).thenReturn(source);
        session.setAttribute("testFlag", 1);
        session.setAttribute(TestController.TIMER_START, 1L);
        session.setAttribute(TestController.TIMER_LIMIT, 100L);

        controller.startBookmarks(new ExtendedModelMap());

        List<?> selected = (List<?>) session.getAttribute("question");
        assertEquals(3, selected.size());
        assertTrue(selected.containsAll(source));
        assertNull(session.getAttribute("testFlag"));
        assertNull(session.getAttribute(TestController.TIMER_START));
        assertNull(session.getAttribute(TestController.TIMER_LIMIT));
    }

    @Test
    void emptyBookmarksReturnToTheListWithoutLeavingAnOldTest() {
        when(favorites.findBookmarkedQuestions(7)).thenReturn(List.of());
        session.setAttribute("question", questions(2));
        session.setAttribute("ans", List.of());
        session.setAttribute("testFlag", 1);
        session.setAttribute(TestController.TIMER_START, 1L);

        assertEquals("redirect:/question/bookmarks", controller.startBookmarks(new ExtendedModelMap()));
        assertNull(session.getAttribute("question"));
        assertNull(session.getAttribute("ans"));
        assertNull(session.getAttribute("testFlag"));
        assertNull(session.getAttribute(TestController.TIMER_START));
    }

    @Test
    void anonymousCannotStartOrQueryBookmarks() {
        session.removeAttribute("user");
        assertEquals("redirect:/", controller.startBookmarks(new ExtendedModelMap()));
        verifyNoInteractions(favorites);
    }
}
