package jp.co.sss.pr.test_function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.validation.BeanPropertyBindingResult;

import jp.co.sss.pr.bean.CustomerBean;
import jp.co.sss.pr.entity.Category;
import jp.co.sss.pr.entity.Customer;
import jp.co.sss.pr.entity.Question;
import jp.co.sss.pr.form.AnswerForm;
import jp.co.sss.pr.form.AnswerListForm;
import jp.co.sss.pr.form.QuestionForm;
import jp.co.sss.pr.repository.CategoryRepository;
import jp.co.sss.pr.repository.CustomerRepository;
import jp.co.sss.pr.repository.QuestionRepository;
import jp.co.sss.pr.service.WeakQuestionService;

/** テスト画面（カテゴリ／苦手範囲／復習／確認／戻る／結果表示）の正常系・異常系 */
class TestControllerFlowTest {

    private TestController controller;
    private MockHttpSession session;
    private QuestionRepository quesRepo;
    private CategoryRepository catRepo;
    private WeakQuestionService weak;

    @BeforeEach
    void setUp() {
        controller = new TestController();
        session = new MockHttpSession();
        quesRepo = mock(QuestionRepository.class);
        catRepo = mock(CategoryRepository.class);
        weak = mock(WeakQuestionService.class);
        CustomerRepository customers = mock(CustomerRepository.class);
        Customer me = new Customer();
        me.setUserId(1);
        when(customers.getReferenceById(1)).thenReturn(me);
        CustomerBean bean = new CustomerBean();
        bean.setUserId(1);
        session.setAttribute("user", bean);
        controller.session = session;
        controller.quesRpstry = quesRepo;
        controller.categoryRepository = catRepo;
        controller.customerRepository = customers;
        controller.weakQuestionService = weak;
    }

    private List<Question> questions(int n) {
        List<Question> list = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            list.add(new Question());
        }
        return list;
    }

    @Test
    void categorySelectClearsAnyPreviousTestFromTheSession() {
        session.setAttribute("question", questions(1));
        session.setAttribute("ans", List.of());
        session.setAttribute("testFlag", 1);
        ExtendedModelMap model = new ExtendedModelMap();

        assertEquals("test/select_category", controller.selectCategory(model));

        assertNull(session.getAttribute("question"));
        assertNull(session.getAttribute("ans"));
        assertNull(session.getAttribute("testFlag"));
        assertNotNull(model.get("categoryList"));
    }

    @Test
    void categoryStartBuildsTheTestIntoTheSession() {
        Category cat = new Category();
        when(catRepo.getReferenceById(1)).thenReturn(cat);
        when(quesRepo.findAllByCategoryAndDifficulty(cat, 2)).thenReturn(questions(5));
        QuestionForm form = new QuestionForm();
        form.setCategory(1);
        form.setDifficulty(2);

        String view = controller.startCategory(new ExtendedModelMap(), form, false);

        assertEquals("test/show_test", view);
        assertEquals(5, ((List<?>) session.getAttribute("question")).size());
        assertEquals(5, ((List<?>) session.getAttribute("ans")).size());
        assertEquals(1, session.getAttribute("testFlag"));
    }

    @Test
    void categoryStartWithNoQuestionsYieldsAnEmptyTest() {
        when(quesRepo.findAllByCategoryAndDifficulty(any(), any())).thenReturn(List.of());
        QuestionForm form = new QuestionForm();
        form.setCategory(1);
        form.setDifficulty(1);

        controller.startCategory(new ExtendedModelMap(), form, false);

        assertEquals(0, ((List<?>) session.getAttribute("question")).size());
    }

    @Test
    void diagnosticSelectClearsTheSessionAndListsCategories() {
        session.setAttribute("question", questions(1));
        ExtendedModelMap model = new ExtendedModelMap();

        assertEquals("test/select_diagnostic", controller.selectDiagnostic(model));

        assertNull(session.getAttribute("question"));
        assertNotNull(model.get("categoryList"));
    }

    @Test
    void diagnosticStartRequiresThreeOrMoreCategories() {
        for (List<Integer> ids : new List[] { null, List.of(), List.of(1), List.of(1, 2) }) {
            ExtendedModelMap model = new ExtendedModelMap();

            String view = controller.startDiagnostic(ids, 1, model, false);

            assertEquals("test/select_diagnostic", view);
            assertEquals("カテゴリを3つ以上選択してください", model.get("errorMessage"));
        }
        assertNull(session.getAttribute("question"));
        verify(quesRepo, never()).findAllByCategoryInAndDifficulty(any(), any());
    }

    @Test
    void diagnosticStartWithThreeCategoriesBuildsTheTest() {
        List<Category> cats = List.of(new Category(), new Category(), new Category());
        for (int i = 0; i < cats.size(); i++) {
            cats.get(i).setCategoryId(i + 1);
        }
        List<Question> qs = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            Question q = new Question();
            q.setCategory(cats.get(i % 3));
            qs.add(q);
        }
        when(catRepo.findAllById(List.of(1, 2, 3))).thenReturn(cats);
        when(quesRepo.findAllByCategoryInAndDifficulty(cats, 1)).thenReturn(qs);

        String view = controller.startDiagnostic(List.of(1, 2, 3), 1, new ExtendedModelMap(), false);

        assertEquals("test/show_test", view);
        List<?> q = (List<?>) session.getAttribute("question");
        assertTrue(q.size() > 0 && q.size() <= TestController.MAX_QUESTIONS);
        assertEquals(q.size(), ((List<?>) session.getAttribute("ans")).size());
    }

    @Test
    void reviewSelectListsRecentMistakes() {
        List<Question> mistakes = questions(2);
        when(weak.pickRecentMistakes(any(), org.mockito.ArgumentMatchers.anyInt())).thenReturn(mistakes);
        ExtendedModelMap model = new ExtendedModelMap();

        assertEquals("test/select_review", controller.selectReview(model));
        assertEquals(mistakes, model.get("reviewList"));
    }

    @Test
    void reviewStartWithoutMistakesReturnsToTheReviewList() {
        when(weak.pickRecentMistakes(any(), org.mockito.ArgumentMatchers.anyInt())).thenReturn(new ArrayList<>());

        assertEquals("redirect:/test/review", controller.startReview(new ExtendedModelMap()));
        assertNull(session.getAttribute("question"));
    }

    @Test
    void reviewStartBuildsTheTestFromMistakesWithoutATimer() {
        when(weak.pickRecentMistakes(any(), org.mockito.ArgumentMatchers.anyInt())).thenReturn(questions(3));

        assertEquals("test/show_test", controller.startReview(new ExtendedModelMap()));
        assertEquals(3, ((List<?>) session.getAttribute("question")).size());
        assertNull(session.getAttribute(TestController.TIMER_START));
    }

    @Test
    void checkRedirectsToMypageWhenThereIsNoTestInTheSession() {
        AnswerListForm form = new AnswerListForm();
        String view = controller.checkTest(form, new BeanPropertyBindingResult(form, "f"),
                new ExtendedModelMap());
        assertEquals("redirect:/mypage", view);
    }

    @Test
    void checkShowsTheReviewScreenAndKeepsTheAnswers() {
        session.setAttribute("question", questions(1));
        AnswerListForm form = new AnswerListForm();
        AnswerForm a = new AnswerForm();
        a.setAnsOption(2);
        form.setResults(List.of(a));

        String view = controller.checkTest(form, new BeanPropertyBindingResult(form, "f"),
                new ExtendedModelMap());

        assertEquals("test/review_test", view);
        assertEquals(form.getResults(), session.getAttribute("ans"));
    }

    @Test
    void checkWithValidationErrorsReturnsToTheTest() {
        session.setAttribute("question", questions(1));
        AnswerListForm form = new AnswerListForm();
        BeanPropertyBindingResult result = new BeanPropertyBindingResult(form, "f");
        result.rejectValue(null, "x");

        assertEquals("test/show_test", controller.checkTest(form, result, new ExtendedModelMap()));
    }

    @Test
    void backReturnsToTheTestOnlyWhenOneIsInProgress() {
        assertEquals("redirect:/mypage", controller.backTest(new AnswerListForm(), new ExtendedModelMap()));
        session.setAttribute("question", questions(1));
        assertEquals("test/show_test", controller.backTest(new AnswerListForm(), new ExtendedModelMap()));
    }

    @Test
    void timeUpWithoutATimerOrMismatchedAnswersGoesToMypage() {
        AnswerListForm form = new AnswerListForm();
        assertEquals("redirect:/mypage", controller.timeUp(form, new ExtendedModelMap()));

        session.setAttribute(TestController.TIMER_START, 1L);
        session.setAttribute("question", questions(2));
        form.setResults(List.of(new AnswerForm()));
        assertEquals("redirect:/mypage", controller.timeUp(form, new ExtendedModelMap()));
    }

    @Test
    void categoryStartWithoutCategoryOrDifficultyReturnsToTheSelectScreen() {
        QuestionForm form = new QuestionForm();
        assertEquals("redirect:/test/category", controller.startCategory(new ExtendedModelMap(), form, false));
        form.setCategory(1);
        assertEquals("redirect:/test/category", controller.startCategory(new ExtendedModelMap(), form, false));
        assertNull(session.getAttribute("question"));
    }

    // ---- 結果（採点・保存） ----

    private AnswerForm answer(int qid, Integer chosen, int correct, Integer category) {
        AnswerForm a = new AnswerForm();
        a.setQuestionId(qid);
        a.setAnsOption(chosen);
        a.setCorrectOption(correct);
        a.setCategory(category);
        return a;
    }

    private jp.co.sss.pr.repository.AnswerRepository prepareResult(List<AnswerForm> forms) {
        jp.co.sss.pr.repository.AnswerRepository ans = mock(jp.co.sss.pr.repository.AnswerRepository.class);
        jp.co.sss.pr.service.LevelService level = mock(jp.co.sss.pr.service.LevelService.class);
        controller.ansRpstry = ans;
        controller.levelService = level;
        when(level.processTestResult(any(), org.mockito.ArgumentMatchers.anyInt(),
                org.mockito.ArgumentMatchers.anyInt())).thenReturn(new ArrayList<>());
        when(quesRepo.findAllById(any())).thenReturn(new ArrayList<>());
        session.setAttribute("question", questions(forms.size()));
        session.setAttribute("ans", forms);
        return ans;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> storedResult() {
        return (Map<String, Object>) session.getAttribute(TestController.RESULT_VIEW);
    }

    @Test
    void resultWithoutATestInTheSessionRedirectsToMypage() {
        assertEquals("redirect:/mypage", controller.resulttTest(new ExtendedModelMap(), new AnswerListForm()));
        session.setAttribute("ans", List.of());
        session.setAttribute("question", questions(1));
        assertEquals("redirect:/mypage", controller.resulttTest(new ExtendedModelMap(), new AnswerListForm()));
    }

    @Test
    void resultSavesEveryAnswerScoresThemAndClearsTheTest() {
        List<AnswerForm> forms = List.of(
                answer(1, 2, 2, 1), answer(2, 1, 3, 1), answer(3, 4, 4, 2), answer(4, null, 1, 9));
        jp.co.sss.pr.repository.AnswerRepository ans = prepareResult(forms);

        assertEquals("redirect:/test/result", controller.resulttTest(new ExtendedModelMap(), new AnswerListForm()));

        verify(ans, org.mockito.Mockito.times(4)).save(any());
        Map<String, Object> view = storedResult();
        assertEquals(4, view.get("qc"));
        assertEquals(2, view.get("ca"));
        assertEquals(false, view.get("isPerfect"));
        assertEquals("suta_cry.png", view.get("displayCharacter"));
        assertEquals(2, ((int[]) view.get("cateCount"))[0]);
        assertEquals(1, ((int[]) view.get("cateCorrect"))[0]);
        assertEquals(1, ((int[]) view.get("cateCount"))[1]);
        assertNull(session.getAttribute("question"));
        assertNull(session.getAttribute("ans"));
    }

    @Test
    void perfectScoreIsFlaggedAndGetsTheCelebrationMessage() {
        prepareResult(List.of(answer(1, 1, 1, 1), answer(2, 2, 2, 1)));

        controller.resulttTest(new ExtendedModelMap(), new AnswerListForm());

        assertEquals(true, storedResult().get("isPerfect"));
        assertEquals("suta_v_happy.png", storedResult().get("displayCharacter"));
    }

    @Test
    void allWrongAnswersGetTheEncouragingCharacter() {
        prepareResult(List.of(answer(1, 2, 1, 1), answer(2, 3, 1, 1)));

        controller.resulttTest(new ExtendedModelMap(), new AnswerListForm());

        assertEquals(0, storedResult().get("ca"));
        assertEquals("suta_sad.png", storedResult().get("displayCharacter"));
    }

    @Test
    void levelUpOverridesTheCharacterAndStoresNewAchievements() {
        jp.co.sss.pr.repository.CustomerRepository customers = controller.customerRepository;
        Customer me = customers.getReferenceById(1);
        me.setUserLevel(1);
        prepareResult(List.of(answer(1, 1, 1, 1)));
        jp.co.sss.pr.entity.Achievement ach = new jp.co.sss.pr.entity.Achievement();
        when(controller.levelService.processTestResult(any(), org.mockito.ArgumentMatchers.anyInt(),
                org.mockito.ArgumentMatchers.anyInt())).thenAnswer(i -> {
                    me.setUserLevel(2);
                    me.setTotalExp(16L);
                    return List.of(ach);
                });

        controller.resulttTest(new ExtendedModelMap(), new AnswerListForm());

        Map<String, Object> view = storedResult();
        assertEquals(true, view.get("leveledUp"));
        assertEquals("suta_star.png", view.get("displayCharacter"));
        assertEquals(1, view.get("previousLevel"));
        assertEquals(2, view.get("currentLevel"));
        assertEquals(1, view.get("expInLevel"));
        assertEquals(14, view.get("expRemaining"));
        assertEquals(List.of(ach), session.getAttribute("newAchievements"));
    }

    @Test
    void noNewAchievementsLeavesTheSessionUntouched() {
        prepareResult(List.of(answer(1, 1, 1, 1)));

        controller.resulttTest(new ExtendedModelMap(), new AnswerListForm());

        assertNull(session.getAttribute("newAchievements"));
    }

    @Test
    void resultPageRedirectsWithoutAResultAndShowsStoredOne() {
        assertEquals("redirect:/mypage", controller.showResult(new ExtendedModelMap()));

        session.setAttribute(TestController.RESULT_VIEW, Map.of("score", 3));
        ExtendedModelMap model = new ExtendedModelMap();

        assertEquals("test/result_test", controller.showResult(model));
        assertEquals(3, model.get("score"));
    }
}
