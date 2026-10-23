package jp.co.sss.pr.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import jp.co.sss.pr.entity.Answer;
import jp.co.sss.pr.entity.Category;
import jp.co.sss.pr.entity.Customer;
import jp.co.sss.pr.entity.Question;
import jp.co.sss.pr.repository.AnswerRepository;
import jp.co.sss.pr.repository.CategoryRepository;
import jp.co.sss.pr.repository.CustomerRepository;
import jp.co.sss.pr.repository.QuestionRepository;
import jp.co.sss.pr.service.WeakQuestionService.WeakQuestion;

@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.sql.init.mode=never"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Import(WeakQuestionService.class)
class WeakQuestionServiceTest {

    @Autowired
    private WeakQuestionService service;

    @Autowired
    private AnswerRepository answerRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private QuestionRepository questionRepository;

    private Customer me;
    private Customer other;
    private Category category;

    @BeforeEach
    void setUp() {
        me = customerRepository.save(customer("me"));
        other = customerRepository.save(customer("other"));
        Category c = new Category();
        c.setCategoryName("Java");
        category = categoryRepository.save(c);
    }

    @Test
    void ordersByLowestCorrectRateAndSkipsPerfectAndRarelyAnsweredQuestions() {
        Question worst = question("worst");
        Question middle = question("middle");
        Question perfect = question("perfect");
        Question once = question("once");

        answers(me, worst, false, false, false);
        answers(me, middle, true, false);
        answers(me, perfect, true, true, true);
        answers(me, once, false);

        List<WeakQuestion> result = service.pick(me, 15);

        assertEquals(List.of("worst", "middle"),
                result.stream().map(w -> w.question().getQuestionText()).toList());
        assertEquals(3, result.get(0).attempts());
        assertEquals(0, result.get(0).correct());
        assertEquals(0, result.get(0).percent());
        assertEquals(50, result.get(1).percent());
    }

    @Test
    void breaksTiesByMoreAttemptsFirst() {
        Question fewer = question("fewer");
        Question more = question("more");
        answers(me, fewer, true, false);
        answers(me, more, true, true, false, false);

        List<WeakQuestion> result = service.pick(me, 15);

        assertEquals("more", result.get(0).question().getQuestionText());
        assertEquals("fewer", result.get(1).question().getQuestionText());
    }

    @Test
    void usesOnlyTheGivenUsersHistory() {
        Question q = question("shared");
        answers(other, q, false, false);
        answers(me, q, true, true);

        assertTrue(service.pick(me, 15).isEmpty());
        assertEquals(1, service.pick(other, 15).size());
    }

    @Test
    void capsTheResultAtTwentyQuestions() {
        for (int i = 0; i < 25; i++) {
            answers(me, question("q" + i), false, false);
        }

        assertEquals(15, service.pick(me, 100).size());
        assertEquals(5, service.pick(me, 5).size());
        assertTrue(service.pick(me, 0).isEmpty());
    }

    @Test
    void returnsNothingWithoutHistory() {
        assertTrue(service.pick(me, 15).isEmpty());
    }

    private Customer customer(String name) {
        Customer customer = new Customer();
        customer.setUserName(name);
        customer.setUserPass("password");
        customer.setPermission(1);
        customer.setDeleteFlag(false);
        customer.setUserLevel(1);
        return customer;
    }

    private Question question(String text) {
        Question q = new Question();
        q.setCategory(category);
        q.setDifficulty(1);
        q.setQuestionText(text);
        q.setOptionA("A");
        q.setOptionB("B");
        q.setOptionC("C");
        q.setCorrectOption(1);
        return questionRepository.save(q);
    }

    private void answers(Customer user, Question question, boolean... results) {
        for (boolean correct : results) {
            Answer a = new Answer();
            a.setCustomer(user);
            a.setQuestion(question);
            a.setAnsOption(correct ? 1 : 2);
            a.setCorrectOption(1);
            a.setCorrectFlag(correct);
            a.setAnsDate(LocalDate.now());
            a.setAnsCount(1);
            answerRepository.save(a);
        }
    }
}
