package jp.co.sss.pr.service;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import jp.co.sss.pr.entity.Customer;
import jp.co.sss.pr.entity.Answer;
import jp.co.sss.pr.entity.Question;
import jp.co.sss.pr.repository.AnswerRepository;
import jp.co.sss.pr.repository.QuestionRepository;
import jp.co.sss.pr.repository.CustomerRepository;

@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.sql.init.mode=never",
        "spring.jpa.show-sql=false"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Import({LevelService.class, QuestService.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class CustomerConcurrencyTest {
    @Autowired private CustomerRepository customers;
    @Autowired private LevelService levels;
    @Autowired private PlatformTransactionManager transactions;
    @Autowired private AnswerRepository answers;
    @Autowired private QuestionRepository questions;

    private void completedQuests(Customer customer) {
        Question question = new Question();
        question.setDifficulty(1);
        question.setQuestionText("Quest test");
        question.setOptionA("A");
        question.setOptionB("B");
        question.setOptionC("C");
        question.setCorrectOption(1);
        questions.saveAndFlush(question);
        for (int i = 0; i < 5; i++) {
            Answer answer = new Answer();
            answer.setCustomer(customer);
            answer.setQuestion(question);
            answer.setCorrectFlag(true);
            answer.setAnsOption(1);
            answer.setCorrectOption(1);
            answer.setAnsDate(LocalDate.now());
            answer.setAnsCount(1);
            answers.saveAndFlush(answer);
        }
    }

    @Test
    void concurrentQuestCompletionsPersistOnlyOneReward() throws Exception {
        Customer original = customer();
        completedQuests(original);
        CountDownLatch loaded = new CountDownLatch(2);
        var executor = Executors.newFixedThreadPool(2);
        try {
            var task = (java.util.concurrent.Callable<Void>) () -> {
                new TransactionTemplate(transactions).executeWithoutResult(status -> {
                    Customer snapshot = customers.findById(original.getUserId()).orElseThrow();
                    loaded.countDown();
                    await(loaded);
                    levels.processTestResult(snapshot, 0, 1);
                });
                return null;
            };
            var first = executor.submit(task);
            var second = executor.submit(task);
            first.get(10, TimeUnit.SECONDS);
            second.get(10, TimeUnit.SECONDS);
            Customer result = customers.findById(original.getUserId()).orElseThrow();
            assertEquals(10L, result.getTotalExp());
            assertEquals(LocalDate.now(), result.getQuestRewardDate());
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void rollbackDoesNotConsumeTheRewardAndRetryCanReceiveIt() {
        Customer original = customer();
        completedQuests(original);
        new TransactionTemplate(transactions).executeWithoutResult(status -> {
            levels.processTestResult(customers.findById(original.getUserId()).orElseThrow(), 0, 1);
            status.setRollbackOnly();
        });
        Customer rolledBack = customers.findById(original.getUserId()).orElseThrow();
        assertEquals(0L, rolledBack.getTotalExp());
        assertNull(rolledBack.getQuestRewardDate());
        levels.processTestResult(rolledBack, 0, 1);
        Customer result = customers.findById(original.getUserId()).orElseThrow();
        assertEquals(10L, result.getTotalExp());
        assertEquals(LocalDate.now(), result.getQuestRewardDate());
    }

    private Customer customer() {
        Customer customer = new Customer();
        customer.setUserName("concurrent");
        customer.setUserPass("test-only");
        customer.setDeleteFlag(false);
        customer.setPermission(0);
        customer.setLoginDate(LocalDate.now().minusDays(1));
        customer.setLoginCount(0);
        return customers.saveAndFlush(customer);
    }

    @Test
    void simultaneousPerfectTestsAccumulateExpAndPerfectCountsWithoutLostUpdates() throws Exception {
        Customer original = customer();
        CountDownLatch loaded = new CountDownLatch(2);
        var executor = Executors.newFixedThreadPool(2);
        try {
            var task = (java.util.concurrent.Callable<Void>) () -> {
                new TransactionTemplate(transactions).executeWithoutResult(status -> {
                    Customer snapshot = customers.findById(original.getUserId()).orElseThrow();
                    loaded.countDown();
                    await(loaded);
                    levels.processTestResult(snapshot, 2, 2);
                });
                return null;
            };
            var first = executor.submit(task);
            var second = executor.submit(task);
            first.get(10, TimeUnit.SECONDS);
            second.get(10, TimeUnit.SECONDS);
            Customer result = customers.findById(original.getUserId()).orElseThrow();
            assertEquals(18L, result.getTotalExp());
            assertEquals(2, result.getPerfectTestCount());
            assertEquals(2, result.getUserLevel());
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void concurrentLoginsCountOnlyOneDay() throws Exception {
        Customer original = customer();
        CountDownLatch loaded = new CountDownLatch(2);
        var executor = Executors.newFixedThreadPool(2);
        try {
            var task = (java.util.concurrent.Callable<Void>) () -> {
                new TransactionTemplate(transactions).executeWithoutResult(status -> {
                    Customer snapshot = customers.findById(original.getUserId()).orElseThrow();
                    loaded.countDown();
                    await(loaded);
                    levels.processLogin(snapshot);
                });
                return null;
            };
            var first = executor.submit(task);
            var second = executor.submit(task);
            first.get(10, TimeUnit.SECONDS);
            second.get(10, TimeUnit.SECONDS);
            assertEquals(1, customers.findById(original.getUserId()).orElseThrow().getLoginCount());
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void staleHeartbeatDoesNotOverwriteNewExpOrProfile() throws Exception {
        Customer original = customer();
        CountDownLatch read = new CountDownLatch(1);
        CountDownLatch updated = new CountDownLatch(1);
        var executor = Executors.newSingleThreadExecutor();
        try {
            var heartbeat = executor.submit(() -> {
                new TransactionTemplate(transactions).executeWithoutResult(status -> {
                    Customer stale = customers.findById(original.getUserId()).orElseThrow();
                    read.countDown();
                    await(updated);
                    stale.setLastSeenAt(LocalDateTime.now());
                    stale.setLoginFlag(true);
                    customers.save(stale);
                });
            });
            assertTrue(read.await(5, TimeUnit.SECONDS));
            new TransactionTemplate(transactions).executeWithoutResult(status -> {
                Customer current = customers.findById(original.getUserId()).orElseThrow();
                levels.processTestResult(current, 2, 2);
                current.setUserName("changed");
                customers.save(current);
            });
            updated.countDown();
            heartbeat.get(10, TimeUnit.SECONDS);
            Customer result = customers.findById(original.getUserId()).orElseThrow();
            assertEquals(9L, result.getTotalExp());
            assertEquals("changed", result.getUserName());
            assertTrue(result.getLoginFlag());
        } finally {
            updated.countDown();
            executor.shutdownNow();
        }
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(5, TimeUnit.SECONDS)) throw new AssertionError("Concurrent operation timed out");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError(e);
        }
    }
}
