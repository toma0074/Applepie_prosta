package jp.co.sss.pr.login_function;

import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

class TabSessionFilterTest {
    private final TabSessionFilter filter = new TabSessionFilter();

    private MockHttpServletResponse call(MockHttpSession browser, String tab, String method,
            String path, FilterChain chain) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(method, "/prosta" + path);
        request.setContextPath("/prosta");
        request.setServletPath(path);
        request.setSession(browser);
        if (tab != null) request.addParameter("_tab", tab);
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, chain);
        return response;
    }

    @Test
    void isolatesUsersAnswersPasswordsAndLogoutInsideOneCookieSession() throws Exception {
        MockHttpSession browser = new MockHttpSession();
        String a = UUID.randomUUID().toString();
        String b = UUID.randomUUID().toString();
        call(browser, a, "POST", "/login", (request, response) -> {
            HttpSession session = ((HttpServletRequest) request).getSession();
            session.setAttribute("user", "Alice");
            session.setAttribute("ans", "Alice answers");
            session.setAttribute("pendingUserPass", "Alice pending");
        });
        call(browser, b, "POST", "/login", (request, response) -> {
            HttpSession session = ((HttpServletRequest) request).getSession();
            assertNull(session.getAttribute("user"));
            assertNull(session.getAttribute("ans"));
            session.setAttribute("user", "Bob");
        });
        call(browser, a, "GET", "/mypage", (request, response) -> {
            HttpSession session = ((HttpServletRequest) request).getSession();
            assertEquals("Alice", session.getAttribute("user"));
            assertEquals("Alice answers", session.getAttribute("ans"));
            assertEquals("Alice pending", session.getAttribute("pendingUserPass"));
        });
        call(browser, b, "GET", "/logout", (request, response) ->
            ((HttpServletRequest) request).getSession().invalidate());
        call(browser, a, "GET", "/mypage", (request, response) ->
            assertEquals("Alice", ((HttpServletRequest) request).getSession().getAttribute("user")));
        assertFalse(browser.isInvalid());
        assertNull(browser.getAttribute("user"));
    }

    @Test
    void rotationClearsOldStateAndOldPagesCannotReachNewIdentity() throws Exception {
        MockHttpSession browser = new MockHttpSession();
        String old = UUID.randomUUID().toString();
        AtomicReference<String> rotated = new AtomicReference<>();
        call(browser, old, "POST", "/login", (request, response) -> {
            HttpServletRequest req = (HttpServletRequest) request;
            req.getSession().setAttribute("ans", "old answer");
            req.getSession().setAttribute("pendingUserPass", "old password");
            rotated.set(req.changeSessionId());
            assertNull(req.getSession().getAttribute("ans"));
            assertNull(req.getSession().getAttribute("pendingUserPass"));
            req.getSession().setAttribute("user", "Bob");
            ((HttpServletResponse) response).sendRedirect("/prosta/mypage");
        });
        assertNotEquals(old, rotated.get());
        call(browser, old, "POST", "/test/result", (request, response) ->
            assertNull(((HttpServletRequest) request).getSession(false)));
        call(browser, rotated.get(), "GET", "/mypage", (request, response) ->
            assertEquals("Bob", ((HttpServletRequest) request).getSession().getAttribute("user")));
    }

    @Test
    void sameTabIdInAnotherBrowserDoesNotAuthenticate() throws Exception {
        String tab = UUID.randomUUID().toString();
        call(new MockHttpSession(), tab, "POST", "/login", (request, response) ->
            ((HttpServletRequest) request).getSession().setAttribute("user", "Alice"));
        call(new MockHttpSession(), tab, "GET", "/mypage", (request, response) ->
            assertNull(((HttpServletRequest) request).getSession(false)));
    }

    @Test
    void rewritesApplicationUrlsRedirectsAndKeepsAssetsAndExternalUrlsUntouched() throws Exception {
        String tab = UUID.randomUUID().toString();
        MockHttpServletResponse result = call(new MockHttpSession(), tab, "GET", "/mypage", (req, res) -> {
            HttpServletResponse response = (HttpServletResponse) res;
            assertEquals("/prosta/mypage?sort=date&_tab=" + tab,
                    response.encodeURL("/prosta/mypage?sort=date"));
            assertEquals("/prosta/css/common.css", response.encodeURL("/prosta/css/common.css"));
            assertEquals("/prosta/api/friends", response.encodeURL("/prosta/api/friends"));
            assertEquals("https://example.com/", response.encodeURL("https://example.com/"));
            assertEquals("#top", response.encodeURL("#top"));
            response.sendRedirect("/prosta/mypage?sort=date");
        });
        assertEquals("/prosta/mypage?sort=date&_tab=" + tab, result.getRedirectedUrl());
        assertEquals("no-store", result.getHeader("Cache-Control"));
    }

    @Test
    void missingAndInvalidTokensNeverFallBackToBrowserAuthentication() throws Exception {
        MockHttpSession browser = new MockHttpSession();
        browser.setAttribute("user", "legacy user");
        assertTrue(call(browser, null, "GET", "/mypage", (req, res) -> fail())
                .getContentAsString().contains("data-bootstrap=\"true\""));
        assertEquals(400, call(browser, null, "POST", "/test/result", (req, res) -> fail()).getStatus());
        assertEquals(400, call(browser, "not-a-token", "GET", "/mypage", (req, res) -> fail()).getStatus());
    }

    @Test
    void browserNavigationNeedsASingleUseTicketBeforeAnyControllerRuns() throws Exception {
        MockHttpSession browser = new MockHttpSession();
        String tab = UUID.randomUUID().toString();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/prosta/test/category/start");
        request.setContextPath("/prosta");
        request.setSession(browser);
        request.addParameter("_tab", tab);
        request.addHeader("Sec-Fetch-Dest", "document");
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, (req, res) -> fail("Must bootstrap before changing test state"));
        assertTrue(response.getContentAsString().contains("data-bootstrap=\"true\""));

        String json = call(browser, tab, "GET", "/api/tab-navigation", (req, res) -> fail()).getContentAsString();
        String ticket = json.substring("{\"ticket\":\"".length(), json.length() - 2);
        request.addParameter("_nav", ticket);
        filter.doFilter(request, new MockHttpServletResponse(), (req, res) ->
            ((HttpServletResponse) res).setHeader("Reached", "yes"));
        MockHttpServletResponse replay = new MockHttpServletResponse();
        filter.doFilter(request, replay, (req, res) -> fail("Ticket must not be replayed"));
        assertTrue(replay.getContentAsString().contains("data-bootstrap=\"true\""));
    }

    @Test
    void conflictingTokensAreRejectedAndStaticAssetsNeedNoSession() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/prosta/api/friends");
        request.setContextPath("/prosta");
        request.addParameter("_tab", UUID.randomUUID().toString());
        request.addHeader("X-Prosta-Tab", UUID.randomUUID().toString());
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, (req, res) -> fail("Conflicting tokens"));
        assertEquals(400, response.getStatus());

        assertEquals(200, call(new MockHttpSession(), null, "GET", "/css/common.css",
                (req, res) -> assertNull(((HttpServletRequest) req).getSession(false).getAttribute("user")))
                .getStatus());
    }

    @Test
    void concurrentSubmissionsWithinATabAreSerializedButOtherTabsAreNotBlocked() throws Exception {
        MockHttpSession browser = new MockHttpSession();
        String tab = UUID.randomUUID().toString();
        call(browser, tab, "GET", "/test/category/start", (request, response) ->
            ((HttpServletRequest) request).getSession().setAttribute("ans", "answers"));
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        AtomicInteger saves = new AtomicInteger();
        var executor = Executors.newFixedThreadPool(3);
        FilterChain grading = (request, response) -> {
            HttpSession session = ((HttpServletRequest) request).getSession();
            if (session.getAttribute("ans") != null) {
                entered.countDown();
                try {
                    if (!release.await(5, TimeUnit.SECONDS)) throw new AssertionError("Timed out");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new AssertionError(e);
                }
                saves.incrementAndGet();
                session.removeAttribute("ans");
            }
        };
        try {
            var first = executor.submit(() -> call(browser, tab, "POST", "/test/result", grading));
            assertTrue(entered.await(5, TimeUnit.SECONDS));
            var second = executor.submit(() -> call(browser, tab, "POST", "/test/result", grading));
            var other = executor.submit(() -> call(browser, UUID.randomUUID().toString(), "GET", "/mypage",
                    (req, res) -> ((HttpServletResponse) res).setHeader("Independent", "yes")));
            assertEquals("yes", other.get(5, TimeUnit.SECONDS).getHeader("Independent"));
            release.countDown();
            first.get(5, TimeUnit.SECONDS);
            second.get(5, TimeUnit.SECONDS);
            assertEquals(1, saves.get());
        } finally {
            release.countDown();
            executor.shutdownNow();
        }
    }
}
