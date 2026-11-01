package jp.co.sss.pr.service;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import jp.co.sss.pr.entity.Category;
import jp.co.sss.pr.entity.Customer;
import jp.co.sss.pr.entity.FriendConnection;
import jp.co.sss.pr.entity.Question;
import jp.co.sss.pr.form.ChallengeAnswerForm;
import jp.co.sss.pr.form.ChallengeCreateForm;
import jp.co.sss.pr.repository.CategoryRepository;
import jp.co.sss.pr.repository.CustomerRepository;
import jp.co.sss.pr.repository.FriendConnectionRepository;
import jp.co.sss.pr.repository.QuestionRepository;

@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.sql.init.mode=never",
        "spring.jpa.show-sql=false"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Import({ChallengeService.class, LevelService.class, QuestService.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ChallengeServiceConcurrencyTest {
    @Autowired private ChallengeService challenges;
    @Autowired private CustomerRepository customers;
    @Autowired private CategoryRepository categories;
    @Autowired private QuestionRepository questions;
    @Autowired private FriendConnectionRepository connections;
    @Autowired private JdbcTemplate jdbc;

    @Test
    void concurrentDuplicateSubmissionsAreGradedAndRewardedOnlyOnce() throws Exception {
        Customer me = customer("CM");
        Customer friend = customer("CF");
        Category category = new Category();
        category.setCategoryName("同時提出" + UUID.randomUUID().toString().substring(0, 5));
        category = categories.saveAndFlush(category);
        for (int i = 1; i <= 5; i++) {
            Question q = new Question();
            q.setCategory(category);
            q.setDifficulty(2);
            q.setQuestionText("同時提出問題" + i);
            q.setOptionA("A");
            q.setOptionB("B");
            q.setOptionC("C");
            q.setCorrectOption(1);
            questions.saveAndFlush(q);
        }
        FriendConnection c = new FriendConnection();
        c.setUserLow(me.getUserId() < friend.getUserId() ? me : friend);
        c.setUserHigh(me.getUserId() < friend.getUserId() ? friend : me);
        c.setRequestedBy(me);
        c.setStatus(FriendConnection.ACCEPTED);
        c.setCreatedAt(LocalDateTime.now());
        c.setUpdatedAt(LocalDateTime.now());
        connections.saveAndFlush(c);

        ChallengeCreateForm form = new ChallengeCreateForm();
        form.setFriendUserId(friend.getUserId());
        form.setCategoryId(category.getCategoryId());
        form.setDifficulty(2);
        form.setQuestionCount(5);
        Integer challengeId = challenges.create(me.getUserId(), form);

        Map<String, String> params = new HashMap<>();
        for (int i = 1; i <= 5; i++) {
            params.put("answer" + i, "1");
        }
        ChallengeAnswerForm answers = ChallengeAnswerForm.fromParameters(params);
        long expBefore = exp(me);

        CountDownLatch start = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);
        List<Future<ChallengeService.SubmitResult>> futures = new ArrayList<>();
        try {
            Callable<ChallengeService.SubmitResult> task = () -> {
                start.await(5, TimeUnit.SECONDS);
                return challenges.submit(challengeId, me.getUserId(), answers);
            };
            futures.add(executor.submit(task));
            futures.add(executor.submit(task));
            start.countDown();
            int succeeded = 0;
            int rejected = 0;
            for (Future<ChallengeService.SubmitResult> f : futures) {
                try {
                    assertEquals(5, f.get(30, TimeUnit.SECONDS).correctCount());
                    succeeded++;
                } catch (ExecutionException e) {
                    assertInstanceOf(ChallengeService.AlreadySubmittedException.class, e.getCause(), e.getCause().toString());
                    rejected++;
                }
            }
            assertEquals(1, succeeded);
            assertEquals(1, rejected);
        } finally {
            executor.shutdownNow();
        }

        assertEquals(5, jdbc.queryForObject("select count(*) from friend_challenge_answer a join friend_challenge_participant p on a.participant_id = p.participant_id where p.challenge_id = ?", Integer.class, challengeId));
        assertEquals(5, jdbc.queryForObject("select count(*) from answer where user_id = ?", Integer.class, me.getUserId()));
        // 5問正解 x2 + 満点ボーナス5 (+ クエスト報酬があればそれ以上) を1回分だけ
        long gained = exp(me) - expBefore;
        assertTrue(gained >= 15 && gained < 30, "EXP must be granted exactly once: " + gained);
    }

    private long exp(Customer c) {
        return jdbc.queryForObject("select total_exp from customer where user_id = ?", Long.class, c.getUserId());
    }

    private Customer customer(String prefix) {
        Customer c = new Customer();
        c.setUserName(prefix + UUID.randomUUID().toString().substring(0, 5));
        c.setUserPass("x");
        c.setPermission(0);
        c.setDeleteFlag(false);
        c.setUserLevel(1);
        c.setTotalExp(0L);
        c.setLoginDate(LocalDate.now());
        c.setLoginFlag(false);
        return customers.saveAndFlush(c);
    }
}