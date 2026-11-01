package jp.co.sss.pr.validation;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import jp.co.sss.pr.entity.Answer;
import jp.co.sss.pr.entity.Category;
import jp.co.sss.pr.entity.Customer;
import jp.co.sss.pr.entity.Favorite;
import jp.co.sss.pr.entity.Question;
import jp.co.sss.pr.repository.FavoriteRepository;

class QuizModesRuntimeTest extends ValidationSupport {
    @Autowired FavoriteRepository favorites;

    static Stream<Arguments> modes() {
        return Stream.of("category", "diagnostic", "weak", "review", "bookmarks", "spaced")
                .flatMap(mode -> Stream.of(0, 1).flatMap(role -> Stream.of(5, 10, 15)
                        .map(count -> Arguments.of(mode, role, count))));
    }

    @ParameterizedTest(name = "{0}, permission={1}, count={2}")
    @MethodSource("modes")
    void allModesUseRequestedOrAvailableCountAndPersistAuthoritativeGrading(
            String mode, int role, int count) throws Exception {
        Customer actor = role == 1 ? admin : user;
        List<Question> source = new ArrayList<>(questions.findAllByCategoryAndDifficulty(category, 1));
        List<Category> diagnosticCategories = new ArrayList<>(List.of(category));
        if ("diagnostic".equals(mode)) {
            for (int i = 0; i < 2; i++) {
                Category c = new Category();
                c.setCategoryName("Quiz" + java.util.UUID.randomUUID().toString().substring(0, 6));
                c = categories.saveAndFlush(c);
                diagnosticCategories.add(c);
                Question q = new Question();
                q.setCategory(c); q.setDifficulty(1); q.setQuestionText("Diagnostic integer question");
                q.setOptionA("int"); q.setOptionB("String"); q.setOptionC("double"); q.setCorrectOption(1);
                source.add(questions.saveAndFlush(q));
            }
        }
        for (Question q : source) {
            if (List.of("weak", "review", "spaced").contains(mode)) {
                for (int i = 0; i < 2; i++) {
                    Answer a = new Answer();
                    a.setCustomer(actor); a.setQuestion(q); a.setAnsDate(LocalDate.now().minusDays(2 - i));
                    a.setAnsOption(2); a.setCorrectOption(1); a.setCorrectFlag(false); a.setAnsCount(1);
                    answers.saveAndFlush(a);
                }
            }
            if ("bookmarks".equals(mode)) {
                Favorite f = new Favorite();
                f.setCustomer(actor); f.setQuestion(q);
                favorites.saveAndFlush(f);
            }
        }
        mvc.perform(get("/test/" + mode + "/start").session(browser)
                        .param("_tab", java.util.UUID.randomUUID().toString()).param("questionCount", "5"))
                .andExpect(status().is3xxRedirection());
        tab = login(actor);
        long previousAnswers = answers.findAllByCustomerOrderByAnsIdDesc(actor).size();
        var start = get("/test/" + mode + "/start").session(browser).param("_tab", tab)
                .param("questionCount", Integer.toString(count)).param("difficulty", "1");
        if ("category".equals(mode)) start.param("category", category.getCategoryId().toString());
        if ("diagnostic".equals(mode)) start.param("categoryIds",
                diagnosticCategories.stream().map(c -> c.getCategoryId().toString()).toArray(String[]::new));
        String html = mvc.perform(start).andExpect(status().isOk()).andExpect(view().name("test/show_test"))
                .andReturn().getResponse().getContentAsString();
        assertFalse(html.contains("name=\"results[0].correctOption\""));
        int actual = submitRenderedQuiz(html, 1);
        assertEquals(Math.min(count, source.size()), actual);
        assertEquals(previousAnswers + actual, answers.findAllByCustomerOrderByAnsIdDesc(actor).size());
        assertEquals(actual, answers.countByCustomerAndCorrectFlagTrue(actor));
        long savedCount = answers.findAllByCustomerOrderByAnsIdDesc(actor).size();
        mvc.perform(post("/test/result").session(browser).param("_tab", tab))
                .andExpect(status().is3xxRedirection());
        assertEquals(savedCount, answers.findAllByCustomerOrderByAnsIdDesc(actor).size());
        protectedPage("/test/result");
    }
}
