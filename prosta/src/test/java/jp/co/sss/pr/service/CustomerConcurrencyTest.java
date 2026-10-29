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
import jp.co.sss.pr.repository.CustomerRepository;

@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.sql.init.mode=never",
        "spring.jpa.show-sql=false"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Import(LevelService.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class CustomerConcurrencyTest {
    @Autowired private CustomerRepository customers;
    @Autowired private LevelService levels;
    @Autowired private PlatformTransactionManager transactions;

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
