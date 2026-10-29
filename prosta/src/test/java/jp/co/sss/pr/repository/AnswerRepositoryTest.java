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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import jp.co.sss.pr.entity.Answer;
import jp.co.sss.pr.entity.Category;
import jp.co.sss.pr.entity.Customer;
import jp.co.sss.pr.entity.Question;
import jp.co.sss.pr.repository.CategoryStudySummary;
import jp.co.sss.pr.repository.RecentStudyActivity;

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
                null,
                LocalDate.now().minusDays(2),
                LocalDate.now());
        Map<Integer, FriendStudySummary> byUser = summaries.stream()
                .collect(Collectors.toMap(FriendStudySummary::getUserId, summary -> summary));

        assertEquals(2, byUser.get(first.getUserId()).getAnswerCount());
        assertEquals(1, byUser.get(first.getUserId()).getCorrectCount());
        assertEquals(0, byUser.get(second.getUserId()).getAnswerCount());
    }

    @Test
    void searchHistoryFiltersByCorrectnessKeywordAndDateRange() {
        Customer user = customerRepository.save(customer("history"));
        Category category = new Category();
        category.setCategoryName("Java");
        category = categoryRepository.save(category);
        Question question = new Question();
        question.setCategory(category);
        question.setDifficulty(2);
        question.setQuestionText("整数を表す型は？");
        question.setOptionA("A");
        question.setOptionB("B");
        question.setOptionC("C");
        question.setCorrectOption(1);
        question = questionRepository.save(question);

        answerRepository.save(answer(user, question, true, LocalDate.now()));
        answerRepository.save(answer(user, question, false, LocalDate.now()));
        answerRepository.save(answer(user, question, false, LocalDate.now().minusDays(10)));

        PageRequest paging = PageRequest.of(0, 6, Sort.by("ansId").descending());
        Integer cid = category.getCategoryId();

        assertEquals(3, answerRepository.searchHistory(user, null, null, "%%", null, null, null, paging)
                .getTotalElements());
        assertEquals(2, answerRepository.searchHistory(user, cid, 2, "%整数%", 0, null, null, paging)
                .getTotalElements());
        assertEquals(1, answerRepository.searchHistory(user, null, null, "%%", 0,
                LocalDate.now().minusDays(2), LocalDate.now(), paging).getTotalElements());
        assertEquals(0, answerRepository.searchHistory(user, null, 3, "%%", null, null, null, paging)
                .getTotalElements());
    }

    @Test
    void summarizesOwnAnswersByCategoryAndReturnsRecentActivity() {
        Customer user = customerRepository.save(customer("learning-status"));
        Customer other = customerRepository.save(customer("other-user"));
        Category java = new Category();
        java.setCategoryName("Java");
        java = categoryRepository.save(java);
        Question question = question(java, "Java question");
        question = questionRepository.save(question);

        answerRepository.save(answer(user, question, true, LocalDate.now()));
        answerRepository.save(answer(user, question, false, LocalDate.now()));
        answerRepository.save(answer(other, question, true, LocalDate.now()));

        List<CategoryStudySummary> summaries = answerRepository.summarizeByCategory(user);
        assertEquals(1, summaries.size());
        assertEquals(java.getCategoryId(), summaries.get(0).getCategoryId());
        assertEquals(2L, summaries.get(0).getAnswerCount());
        assertEquals(1L, summaries.get(0).getCorrectCount());

        List<RecentStudyActivity> activities = answerRepository
                .findRecentActivities(user, PageRequest.of(0, 10))
                .getContent();
        assertEquals(2, activities.size());
        assertEquals("Java", activities.get(0).getCategoryName());
        assertEquals("Java question", activities.get(0).getQuestionText());
    }

    private Customer customer(String name) {
        Customer customer = new Customer();
        customer.setUserName(name);
        customer.setUserPass("password");
        customer.setPermission(0);
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

    private Question question(Category category, String text) {
        Question question = new Question();
        question.setCategory(category);
        question.setDifficulty(1);
        question.setQuestionText(text);
        question.setOptionA("A");
        question.setOptionB("B");
        question.setOptionC("C");
        question.setCorrectOption(1);
        return question;
    }
}
