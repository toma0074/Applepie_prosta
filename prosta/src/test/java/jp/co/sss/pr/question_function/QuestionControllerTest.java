package jp.co.sss.pr.question_function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.ui.ExtendedModelMap;

import jp.co.sss.pr.bean.CustomerBean;
import jp.co.sss.pr.entity.Category;
import jp.co.sss.pr.entity.Customer;
import jp.co.sss.pr.entity.Favorite;
import jp.co.sss.pr.entity.Question;
import jp.co.sss.pr.repository.CategoryRepository;
import jp.co.sss.pr.repository.CustomerRepository;
import jp.co.sss.pr.repository.FavoriteRepository;
import jp.co.sss.pr.repository.QuestionRepository;

/** 問題一覧・各種検索の正常系／異常系 */
class QuestionControllerTest {

    private QuestionRepository qRepo;
    private CategoryRepository cRepo;
    private FavoriteRepository favRepo;
    private CustomerRepository custRepo;
    private MockHttpSession session;
    private QuestionController controller;
    private final List<Question> all = List.of(new Question(), new Question());

    @BeforeEach
    void setUp() {
        qRepo = mock(QuestionRepository.class);
        cRepo = mock(CategoryRepository.class);
        favRepo = mock(FavoriteRepository.class);
        custRepo = mock(CustomerRepository.class);
        session = new MockHttpSession();
        controller = new QuestionController();
        controller.qRepository = qRepo;
        controller.categoryRepository = cRepo;
        controller.favoriteRepository = favRepo;
        controller.customerRepository = custRepo;
        controller.session = session;
        when(qRepo.findAllByOrderByDifficultyAscCategoryAsc()).thenReturn(all);
    }

    @Test
    void listShowsAllQuestions() {
        ExtendedModelMap model = new ExtendedModelMap();
        assertEquals("question/question_list", controller.questionFindAll(model));
        assertEquals(all, model.get("Questions"));
        assertEquals("全件検索", model.get("QuestionResult"));
        // 未ログインでも★表示用の集合は空で渡す
        assertEquals(Set.of(), model.get("favoriteIds"));
    }

    @Test
    void listMarksFavoritedQuestionsOfTheLoggedInUser() {
        CustomerBean bean = new CustomerBean();
        bean.setUserId(7);
        session.setAttribute("user", bean);
        Customer customer = new Customer();
        when(custRepo.findById(7)).thenReturn(Optional.of(customer));
        Question q = new Question();
        q.setQuestionId(42);
        Favorite fav = new Favorite();
        fav.setQuestion(q);
        when(favRepo.findByCustomer(customer)).thenReturn(List.of(fav));
        ExtendedModelMap model = new ExtendedModelMap();

        controller.questionFindAll(model);

        assertEquals(Set.of(42), model.get("favoriteIds"));
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
