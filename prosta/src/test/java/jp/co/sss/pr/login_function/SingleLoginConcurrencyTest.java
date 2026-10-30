package jp.co.sss.pr.login_function;

import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jp.co.sss.pr.bean.CustomerBean;
import jp.co.sss.pr.entity.Customer;
import jp.co.sss.pr.repository.CustomerRepository;

@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop", "spring.sql.init.mode=never"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class SingleLoginConcurrencyTest {
    @Autowired EntityManager entityManager;
    @Autowired PlatformTransactionManager transactions;
    @Autowired CustomerRepository customers;

    @Test
    void queuedOldUpdateIsRejectedAfterNewLoginCommits() throws Exception {
        Customer customer = new Customer();
        customer.setUserName("lock");
        customer.setUserPass("test-only");
        customer.setPermission(0);
        customer.setDeleteFlag(false);
        customer.setActiveLoginToken(UUID.randomUUID().toString());
        customer = customers.saveAndFlush(customer);
        int id = customer.getUserId();
        CustomerBean user = new CustomerBean();
        user.setUserId(id);
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("user", user);
        session.setAttribute(SingleLoginFilter.LOGIN_TOKEN, customer.getActiveLoginToken());
        CountDownLatch locked = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        CountDownLatch attempted = new CountDownLatch(1);
        AtomicBoolean saved = new AtomicBoolean();
        var executor = Executors.newFixedThreadPool(2);
        try {
            var newLogin = executor.submit(() -> new TransactionTemplate(transactions).executeWithoutResult(status -> {
                Customer current = entityManager.find(Customer.class, id, LockModeType.PESSIMISTIC_WRITE);
                current.setActiveLoginToken(UUID.randomUUID().toString());
                locked.countDown();
                await(release);
            }));
            assertTrue(locked.await(5, TimeUnit.SECONDS));
            var oldUpdate = executor.submit(() -> {
                MockHttpServletRequest request = new MockHttpServletRequest("POST", "/test/result");
                request.setSession(session);
                MockHttpServletResponse response = new MockHttpServletResponse();
                attempted.countDown();
                new SingleLoginFilter(entityManager, transactions).doFilter(request, response,
                        (req, res) -> saved.set(true));
                return response;
            });
            assertTrue(attempted.await(5, TimeUnit.SECONDS));
            release.countDown();
            newLogin.get(10, TimeUnit.SECONDS);
            assertEquals(302, oldUpdate.get(10, TimeUnit.SECONDS).getStatus());
            assertFalse(saved.get());
            assertTrue(session.isInvalid());
        } finally {
            release.countDown();
            executor.shutdownNow();
        }
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(5, TimeUnit.SECONDS)) throw new AssertionError("Timed out");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError(e);
        }
    }
}
