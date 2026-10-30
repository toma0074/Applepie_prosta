package jp.co.sss.pr.validation;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;
import jp.co.sss.pr.entity.Customer;
import jp.co.sss.pr.entity.FriendConnection;
import jp.co.sss.pr.entity.Question;

class LargeDataRuntimeTest extends ValidationSupport {
    private static final int QUESTION_COUNT = 2_000;
    private static final int ANSWER_COUNT = 50_000;
    private static final int FRIEND_COUNT = 50;
    private static final int WORKERS = 4;
    private static final int REQUESTS_PER_WORKER = 20;
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper json;

    @Test
    @Timeout(120)
    void largeHistoryPaginationAggregationsAndConcurrentPlanWrites() throws Exception {
        assertTrue(jdbc.execute((java.sql.Connection c) -> c.getMetaData().getURL()).startsWith("jdbc:h2:"));
        List<Customer> extraUsers = new ArrayList<>();
        List<Question> extraQuestions = new ArrayList<>();
        List<Long> timings = Collections.synchronizedList(new ArrayList<>());
        LocalDate today = LocalDate.now();
        long seedStart = System.nanoTime();
        try {
            for (int i = 0; i < QUESTION_COUNT; i++) {
                Question q = new Question();
                q.setCategory(category);
                q.setDifficulty(1);
                q.setQuestionText("load-" + i);
                q.setOptionA("A");
                q.setOptionB("B");
                q.setOptionC("C");
                q.setCorrectOption(1);
                extraQuestions.add(q);
            }
            extraQuestions = questions.saveAllAndFlush(extraQuestions);
            for (int i = 0; i < FRIEND_COUNT - 1; i++) {
                Customer c = new Customer();
                c.setUserName("L" + i + user.getUserName().substring(1, 4));
                c.setUserPass(user.getUserPass());
                c.setPermission(0);
                c.setDeleteFlag(false);
                c.setUserLevel(1);
                extraUsers.add(c);
            }
            extraUsers = customers.saveAllAndFlush(extraUsers);
            List<Customer> allFriends = new ArrayList<>(extraUsers);
            allFriends.add(friend);
            for (Customer other : allFriends) {
                FriendConnection connection = new FriendConnection();
                connection.setUserLow(user);
                connection.setUserHigh(other);
                connection.setRequestedBy(user);
                connection.setStatus(FriendConnection.ACCEPTED);
                connection.setCreatedAt(LocalDateTime.now());
                connection.setUpdatedAt(LocalDateTime.now());
                connections.save(connection);
            }
            // H2専用の一括投入。実DBの設定・データには接続しない。
            List<Object[]> rows = new ArrayList<>();
            for (int i = 0; i < ANSWER_COUNT; i++) {
                rows.add(new Object[] {i < ANSWER_COUNT / 2 ? user.getUserId() : friend.getUserId(),
                        extraQuestions.get(i % QUESTION_COUNT).getQuestionId(), i % 2 == 0 ? 1 : 2,
                        i % 2 == 0, today.minusDays(i % 60)});
            }
            jdbc.batchUpdate("""
                    INSERT INTO answer (ans_id, user_id, question_id, ans_option, correct_option,
                                        correct_flag, ans_date, ans_count)
                    VALUES (NEXT VALUE FOR seq_answer, ?, ?, ?, 1, ?, ?, 1)
                    """, rows);
            long seedMillis = elapsedMillis(seedStart);
            assertEquals(ANSWER_COUNT, jdbc.queryForObject(
                    "SELECT COUNT(*) FROM answer WHERE user_id IN (?, ?)", Long.class,
                    user.getUserId(), friend.getUserId()));
            tab = login(user);
            protectedPage("/mypage/history");
            protectedPage("/learning-status");

            var history = measured(get("/mypage/history"), timings).getModelAndView();
            assertNotNull(history);
            Page<?> page = assertInstanceOf(Page.class, history.getModel().get("historyPage"));
            assertEquals(25_000, page.getTotalElements());
            assertEquals(6, page.getContent().size());
            var last = measured(get("/mypage/history").param("page", "4166"), timings).getModelAndView();
            assertNotNull(last);
            assertEquals(4, assertInstanceOf(List.class, last.getModel().get("answerlist")).size());
            var learning = measured(get("/learning-status"), timings).getModelAndView();
            assertNotNull(learning);
            assertEquals(25_000L, learning.getModel().get("totalAnswers"));
            assertEquals(12_500L, learning.getModel().get("totalCorrect"));
            assertEquals(15, assertInstanceOf(List.class, learning.getModel().get("recentActivities")).size());
            var weak = measured(get("/test/weak"), timings).getModelAndView();
            assertNotNull(weak);
            assertEquals(15, assertInstanceOf(List.class, weak.getModel().get("weakList")).size());
            var review = measured(get("/test/review"), timings).getModelAndView();
            assertNotNull(review);
            assertEquals(15, assertInstanceOf(List.class, review.getModel().get("reviewList")).size());
            var feed = measured(get("/api/friends/" + friend.getUserId() + "/feed"), timings);
            assertEquals(30, json.readTree(feed.getResponse().getContentAsString()).size());
            var friends = json.readTree(measured(get("/api/friends"), timings).getResponse().getContentAsString());
            assertEquals(FRIEND_COUNT, friends.size());
            var comparison = json.readTree(measured(get("/api/friends/leaderboard")
                    .param("from", today.minusDays(59).toString()).param("to", today.toString()),
                    timings).getResponse().getContentAsString());
            assertEquals(FRIEND_COUNT + 1, comparison.get("entries").size());
            long comparedAnswers = 0;
            long comparedCorrect = 0;
            for (var entry : comparison.get("entries")) {
                comparedAnswers += entry.get("answerCount").asLong();
                comparedCorrect += entry.get("correctCount").asLong();
            }
            assertEquals(ANSWER_COUNT, comparedAnswers);
            assertEquals(ANSWER_COUNT / 2, comparedCorrect);
            String adminTab = login(admin);
            long adminBegin = System.nanoTime();
            var adminPage = mvc.perform(get("/admin/questions").session(browser).param("_tab", adminTab)
                            .param("categoryId", category.getCategoryId().toString()))
                    .andExpect(status().isOk()).andReturn().getModelAndView();
            timings.add(elapsedMillis(adminBegin));
            assertNotNull(adminPage);
            Page<?> questionsPage = assertInstanceOf(Page.class, adminPage.getModel().get("page"));
            assertEquals(QUESTION_COUNT + 8, questionsPage.getTotalElements());
            assertEquals(10, questionsPage.getContent().size());

            List<Customer> actors = List.of(user, friend, extraUsers.get(0), extraUsers.get(1));
            runConcurrentRequests(actors, today, timings);
            for (Customer actor : actors) {
                assertEquals(REQUESTS_PER_WORKER / 2,
                        plans.findAllByCustomerAndPlanDateOrderByStudyPlanIdAsc(actor, today).size());
            }
            assertEquals(25_000, answers.countByCustomerAndCorrectFlagTrue(user)
                    + answers.countByCustomerAndCorrectFlagTrue(friend));
            List<Long> sorted = timings.stream().sorted().toList();
            long p95 = sorted.get((int) Math.ceil(sorted.size() * 0.95) - 1);
            long max = sorted.get(sorted.size() - 1);
            System.out.printf("LOAD_RESULT questions=%d answers=%d friends=%d workers=%d requests=%d"
                            + " measured=%d seed_ms=%d p95_ms=%d max_ms=%d%n",
                    QUESTION_COUNT, ANSWER_COUNT, FRIEND_COUNT, WORKERS, WORKERS * REQUESTS_PER_WORKER,
                    sorted.size(), seedMillis, p95, max);
            assertTrue(p95 < 5_000, "Local regression budget exceeded: p95=" + p95 + "ms");
            assertTrue(max < 10_000, "Local regression budget exceeded: max=" + max + "ms");
        } finally {
            for (Customer actor : List.of(user, friend)) {
                jdbc.update("DELETE FROM study_plan WHERE user_id = ?", actor.getUserId());
                jdbc.update("DELETE FROM answer WHERE user_id = ?", actor.getUserId());
            }
            jdbc.update("DELETE FROM friend_connection WHERE user_low_id = ? OR user_high_id = ?",
                    user.getUserId(), user.getUserId());
            for (Customer extra : extraUsers) {
                jdbc.update("DELETE FROM study_plan WHERE user_id = ?", extra.getUserId());
            }
            questions.deleteAllInBatch(extraQuestions);
            customers.deleteAllInBatch(extraUsers);
        }
    }

    private void runConcurrentRequests(List<Customer> actors, LocalDate today, List<Long> timings) throws Exception {
        var executor = Executors.newFixedThreadPool(WORKERS);
        var start = new CountDownLatch(1);
        List<java.util.concurrent.Future<Void>> futures = new ArrayList<>();
        try {
            for (Customer actor : actors) {
                MockHttpSession actorBrowser = new MockHttpSession();
                String redirect = mvc.perform(post("/login").session(actorBrowser)
                                .param("_tab", java.util.UUID.randomUUID().toString())
                                .param("userName", actor.getUserName()).param("userPass", "pass1234"))
                        .andExpect(status().is3xxRedirection()).andReturn().getResponse().getRedirectedUrl();
                assertNotNull(redirect);
                String actorTab = UriComponentsBuilder.fromUriString(redirect).build().getQueryParams().getFirst("_tab");
                assertNotNull(actorTab);
                futures.add(executor.submit(() -> {
                    assertTrue(start.await(10, TimeUnit.SECONDS));
                    for (int i = 0; i < REQUESTS_PER_WORKER; i++) {
                        boolean write = i % 2 == 0;
                        var request = write
                                ? post("/study-calendar/plans").param("planDate", today.toString())
                                        .param("title", "load-" + i)
                                : get("/api/friends/me");
                        long begin = System.nanoTime();
                        mvc.perform(request.session(actorBrowser).param("_tab", actorTab))
                                .andExpect(write ? status().is3xxRedirection() : status().isOk());
                        timings.add(elapsedMillis(begin));
                    }
                    return null;
                }));
            }
            long begin = System.nanoTime();
            start.countDown();
            for (var future : futures) future.get(60, TimeUnit.SECONDS);
            assertTrue(elapsedMillis(begin) < 60_000, "Concurrent workload exceeded 60 seconds");
        } finally {
            start.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS), "Workers did not stop");
        }
    }

    private MvcResult measured(MockHttpServletRequestBuilder request, List<Long> timings) throws Exception {
        long begin = System.nanoTime();
        MvcResult result = mvc.perform(request.session(browser).param("_tab", tab))
                .andExpect(status().isOk()).andReturn();
        long millis = elapsedMillis(begin);
        timings.add(millis);
        System.out.printf("LOAD_ENDPOINT path=%s elapsed_ms=%d%n", result.getRequest().getRequestURI(), millis);
        return result;
    }

    private static long elapsedMillis(long begin) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - begin);
    }
}
