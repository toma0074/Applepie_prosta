package jp.co.sss.pr.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import jp.co.sss.pr.entity.Answer;
import jp.co.sss.pr.entity.Category;
import jp.co.sss.pr.entity.Customer;
import jp.co.sss.pr.entity.Question;

@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.sql.init.mode=never"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class AnswerRepositoryTest {

    @Autowired
    private AnswerRepository answerRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @Test
    void summarizesAnswersForFriendsByCategoryAndDateRange() {
        Customer first = customerRepository.save(customer("first"));
        Customer second = customerRepository.save(customer("second"));
        Category category = new Category();
        category.setCategoryName("Java");
        category = categoryRepository.save(category);

        Question question = new Question();
        question.setCategory(category);
        question.setDifficulty(1);
        question.setQuestionText("Question");
        question.setOptionA("A");
        question.setOptionB("B");
        question.setOptionC("C");
        question.setCorrectOption(1);
        question = questionRepository.save(question);

        answerRepository.save(answer(first, question, true, LocalDate.now()));
        answerRepository.save(answer(first, question, false, LocalDate.now()));
        answerRepository.save(answer(second, question, true, LocalDate.now().minusDays(10)));

        List<FriendStudySummary> summaries = answerRepository.summarizeStudy(
                List.of(first.getUserId(), second.getUserId()),
                category.getCategoryId(),
                LocalDate.now().minusDays(2),
                LocalDate.now());
        Map<Integer, FriendStudySummary> byUser = summaries.stream()
                .collect(Collectors.toMap(FriendStudySummary::getUserId, summary -> summary));

        assertEquals(2, byUser.get(first.getUserId()).getAnswerCount());
        assertEquals(1, byUser.get(first.getUserId()).getCorrectCount());
        assertEquals(0, byUser.get(second.getUserId()).getAnswerCount());
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

    private Answer answer(Customer customer, Question question, boolean correct, LocalDate date) {
        Answer answer = new Answer();
        answer.setCustomer(customer);
        answer.setQuestion(question);
        answer.setAnsOption(correct ? 1 : 2);
        answer.setCorrectOption(1);
        answer.setCorrectFlag(correct);
        answer.setAnsDate(date);
        answer.setAnsCount(1);
        return answer;
    }
}
