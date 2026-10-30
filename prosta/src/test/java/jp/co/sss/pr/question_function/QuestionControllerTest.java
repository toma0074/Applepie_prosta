package jp.co.sss.pr.question_function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ui.ExtendedModelMap;

import jp.co.sss.pr.entity.Category;
import jp.co.sss.pr.entity.Question;
import jp.co.sss.pr.repository.CategoryRepository;
import jp.co.sss.pr.repository.QuestionRepository;

/** 問題一覧・各種検索の正常系／異常系 */
class QuestionControllerTest {

    private QuestionRepository qRepo;
    private CategoryRepository cRepo;
    private QuestionController controller;
    private final List<Question> all = List.of(new Question(), new Question());

    @BeforeEach
    void setUp() {
        qRepo = mock(QuestionRepository.class);
        cRepo = mock(CategoryRepository.class);
        controller = new QuestionController();
        controller.qRepository = qRepo;
        controller.categoryRepository = cRepo;
        when(qRepo.findAllByOrderByDifficultyAscCategoryAsc()).thenReturn(all);
    }

    @Test
    void listShowsAllQuestions() {
        ExtendedModelMap model = new ExtendedModelMap();
        assertEquals("question/question_list", controller.questionFindAll(model));
        assertEquals(all, model.get("Questions"));
        assertEquals("全件検索", model.get("QuestionResult"));
    }

    @Test
    void categorySearchFiltersByTheCategoryAndShowsItsName() {
        Category cat = new Category();
        cat.setCategoryName("ネットワーク");
        when(cRepo.findById(2)).thenReturn(java.util.Optional.of(cat));
        List<Question> hit = List.of(new Question());
        when(qRepo.findByCategoryOrderByDifficultyAscCategoryAsc(cat)).thenReturn(hit);
        ExtendedModelMap model = new ExtendedModelMap();

        controller.questionList(model, 2);

        assertEquals(hit, model.get("Questions"));
        assertEquals("ネットワーク", model.get("QuestionResult"));
    }

    @Test
    void categorySearchWithoutCategoryShowsEverything() {
        ExtendedModelMap model = new ExtendedModelMap();
        controller.questionList(model, null);
        assertEquals(all, model.get("Questions"));
        assertEquals("全件検索", model.get("QuestionResult"));
        verify(cRepo, never()).findById(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void categorySearchWithUnknownCategoryShowsEverything() {
        when(cRepo.findById(99)).thenReturn(java.util.Optional.empty());
        ExtendedModelMap model = new ExtendedModelMap();
        controller.questionList(model, 99);
        assertEquals(all, model.get("Questions"));
        assertEquals("全件検索", model.get("QuestionResult"));
    }

    @Test
    void textSearchUsesThePartialMatchAndEchoesTheKeyword() {
        List<Question> hit = List.of(new Question());
        when(qRepo.findByQuestionTextContainingOrderByDifficultyAscCategoryAsc("TCP")).thenReturn(hit);
        ExtendedModelMap model = new ExtendedModelMap();

        controller.questionText(model, "TCP");

        assertEquals(hit, model.get("Questions"));
        assertEquals("TCP", model.get("QuestionResult"));
    }

    @Test
    void textSearchWithNullOrEmptyKeywordShowsEverything() {
        for (String kw : new String[] { null, "" }) {
            ExtendedModelMap model = new ExtendedModelMap();
            controller.questionText(model, kw);
            assertEquals(all, model.get("Questions"));
            assertEquals("全件検索", model.get("QuestionResult"));
        }
    }

    @Test
    void textSearchWithNoHitReturnsAnEmptyList() {
        when(qRepo.findByQuestionTextContainingOrderByDifficultyAscCategoryAsc("zzz")).thenReturn(List.of());
        ExtendedModelMap model = new ExtendedModelMap();
        controller.questionText(model, "zzz");
        assertEquals(List.of(), model.get("Questions"));
    }

    @Test
    void difficultySearchShowsStarsForEachLevel() {
        String[] stars = { "★☆☆", "★★☆", "★★★" };
        for (int d = 1; d <= 3; d++) {
            List<Question> hit = List.of(new Question());
            when(qRepo.findByDifficultyOrderByCategoryAsc(d)).thenReturn(hit);
            ExtendedModelMap model = new ExtendedModelMap();

            controller.questionResult(model, d);

            assertEquals(stars[d - 1], model.get("QuestionResult"));
            assertEquals(hit, model.get("Questions"));
        }
    }

    @Test
    void difficultySearchWithoutValueShowsEverything() {
        ExtendedModelMap model = new ExtendedModelMap();
        controller.questionResult(model, null);
        assertEquals(all, model.get("Questions"));
        assertEquals("全件検索", model.get("QuestionResult"));
    }
}
