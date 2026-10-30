package jp.co.sss.pr.validation;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.HttpStatusCode;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import jakarta.persistence.EntityManager;
import jp.co.sss.pr.bean.CustomerBean;
import jp.co.sss.pr.entity.Customer;
import jp.co.sss.pr.login_function.SingleLoginFilter;
import jp.co.sss.pr.service.AfterCommit;

class RequestTransactionRuntimeTest extends ValidationSupport {
    @Autowired EntityManager entityManager;
    @Autowired PlatformTransactionManager transactions;
    @LocalServerPort int port;

    @ParameterizedTest
    @ValueSource(ints = {400, 403, 404, 409})
    void resolvedClientErrorRollsBackPartialWritesAndSuppressesAfterCommit(int code) throws Exception {
        var request = authenticatedRequest();
        var response = new MockHttpServletResponse();
        var published = new AtomicBoolean();
        String oldName = user.getUserName();
        new SingleLoginFilter(entityManager, transactions).doFilter(request, response, (req, res) -> {
            try {
                new TransactionTemplate(transactions).executeWithoutResult(status -> {
                    entityManager.find(Customer.class, user.getUserId()).setUserName("changed");
                    entityManager.flush();
                    AfterCommit.run(() -> published.set(true));
                    throw new ResponseStatusException(HttpStatusCode.valueOf(code));
                });
            } catch (ResponseStatusException expected) {
                response.sendError(expected.getStatusCode().value());
            }
        });
        assertEquals(code, response.getStatus());
        assertEquals(oldName, customers.findById(user.getUserId()).orElseThrow().getUserName());
        assertFalse(published.get());
    }

    @Test
    void clientErrorWithoutInnerTransactionAlsoRollsBackPartialWrite() throws Exception {
        var request = authenticatedRequest();
        var response = new MockHttpServletResponse();
        String oldName = user.getUserName();
        new SingleLoginFilter(entityManager, transactions).doFilter(request, response, (req, res) -> {
            entityManager.find(Customer.class, user.getUserId()).setUserName("changed");
            entityManager.flush();
            response.setStatus(400);
        });
        assertEquals(oldName, customers.findById(user.getUserId()).orElseThrow().getUserName());
        assertEquals(400, response.getStatus());
    }

    @Test
    void databaseFailureStillPropagatesAndRollsBack() throws Exception {
        var request = authenticatedRequest();
        String oldName = user.getUserName();
        assertThrows(DataAccessResourceFailureException.class,
                () -> new SingleLoginFilter(entityManager, transactions).doFilter(
                        request, new MockHttpServletResponse(), (req, res) -> {
                            entityManager.find(Customer.class, user.getUserId()).setUserName("changed");
                            entityManager.flush();
                            throw new DataAccessResourceFailureException("controlled validation outage");
                        }));
        assertEquals(oldName, customers.findById(user.getUserId()).orElseThrow().getUserName());
    }

    @Test
    void successfulRequestCommitsAndPublishesAfterCommit() throws Exception {
        var request = authenticatedRequest();
        var published = new AtomicBoolean();
        new SingleLoginFilter(entityManager, transactions).doFilter(
                request, new MockHttpServletResponse(), (req, res) -> {
                    entityManager.find(Customer.class, user.getUserId()).setUserName("changed");
                    AfterCommit.run(() -> published.set(true));
                    assertFalse(published.get());
                });
        assertTrue(published.get());
        assertEquals("changed", customers.findById(user.getUserId()).orElseThrow().getUserName());
    }

    @Test
    void realHttpKeeps400403404409AndAllowsSubsequentRequests() throws Exception {
        var client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5))
                .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL))
                .followRedirects(HttpClient.Redirect.NEVER).build();
        String base = "http://127.0.0.1:" + port + "/prosta";
        String form = "userName=" + URLEncoder.encode(user.getUserName(), StandardCharsets.UTF_8)
                + "&userPass=pass1234&_tab=" + java.util.UUID.randomUUID();
        var login = client.send(HttpRequest.newBuilder(URI.create(base + "/login"))
                .timeout(Duration.ofSeconds(10)).header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form)).build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(302, login.statusCode());
        String token = UriComponentsBuilder.fromUriString(login.headers().firstValue("location").orElseThrow())
                .build().getQueryParams().getFirst("_tab");
        assertNotNull(token);
        assertEquals(400, send(client, base, "/api/friends/requests/" + user.getUserId(), token, "POST"));
        assertEquals(201, send(client, base, "/api/friends/requests/" + friend.getUserId(), token, "POST"));
        var connection = connections.findByUserLowAndUserHigh(user, friend).orElseThrow();
        assertEquals(409, send(client, base, "/api/friends/requests/" + friend.getUserId(), token, "POST"));
        assertEquals(403, send(client, base, "/api/friends/requests/"
                + connection.getFriendConnectionId() + "/accept", token, "POST"));
        assertEquals(404, send(client, base, "/api/friends/requests/-1/accept", token, "POST"));
        assertEquals(200, send(client, base, "/api/friends/requests", token, "GET"));
        assertEquals(jp.co.sss.pr.entity.FriendConnection.PENDING,
                connections.findById(connection.getFriendConnectionId()).orElseThrow().getStatus());
        assertEquals(204, send(client, base, "/api/friends/requests/"
                + connection.getFriendConnectionId(), token, "DELETE"));
        assertTrue(connections.findById(connection.getFriendConnectionId()).isEmpty());
        assertEquals(404, send(client, base, "/api/friends/" + friend.getUserId() + "/feed", token, "GET"));
        assertEquals(200, send(client, base, "/api/friends/me", token, "GET"));
    }

    @Test
    void sameAccountRowLockPreventsLostUpdatesUnderConcurrentRequests() throws Exception {
        var template = authenticatedRequest();
        long oldExp = customers.findById(user.getUserId()).orElseThrow().getTotalExp();
        var executor = Executors.newFixedThreadPool(4);
        var start = new CountDownLatch(1);
        var published = new AtomicInteger();
        var futures = new java.util.ArrayList<java.util.concurrent.Future<Void>>();
        try {
            for (int worker = 0; worker < 4; worker++) {
                futures.add(executor.submit(() -> {
                    assertTrue(start.await(5, TimeUnit.SECONDS));
                    for (int i = 0; i < 20; i++) {
                        var request = new MockHttpServletRequest("POST", "/api/transaction-validation");
                        request.setSession(template.getSession());
                        var response = new MockHttpServletResponse();
                        new SingleLoginFilter(entityManager, transactions).doFilter(request, response, (req, res) -> {
                            Customer current = entityManager.find(Customer.class, user.getUserId());
                            current.setTotalExp(current.getTotalExp() + 1);
                            AfterCommit.run(() -> published.incrementAndGet());
                        });
                        assertEquals(200, response.getStatus());
                    }
                    return null;
                }));
            }
            start.countDown();
            for (var future : futures) future.get(30, TimeUnit.SECONDS);
            assertEquals(oldExp + 80, customers.findById(user.getUserId()).orElseThrow().getTotalExp());
            assertEquals(80, published.get());
        } finally {
            start.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
        }
    }

    private int send(HttpClient client, String base, String path, String token, String method) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create(base + path + "?_tab=" + token))
                .timeout(Duration.ofSeconds(10)).method(method, HttpRequest.BodyPublishers.noBody()).build(),
                HttpResponse.BodyHandlers.ofString()).statusCode();
    }

    private MockHttpServletRequest authenticatedRequest() throws Exception {
        login(user);
        CustomerBean bean = new CustomerBean();
        bean.setUserId(user.getUserId());
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("user", bean);
        session.setAttribute(SingleLoginFilter.LOGIN_TOKEN,
                customers.findById(user.getUserId()).orElseThrow().getActiveLoginToken());
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/transaction-validation");
        request.setSession(session);
        return request;
    }
}
