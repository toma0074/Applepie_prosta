package jp.co.sss.pr.login_function;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;

import jp.co.sss.pr.bean.CustomerBean;

class LoginCheckTest {
    private MockHttpServletResponse call(String path, MockHttpSession session,
            AtomicBoolean reached) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/prosta" + path);
        request.setContextPath("/prosta");
        request.setServletPath(path);
        if (session != null) request.setSession(session);
        MockHttpServletResponse response = new MockHttpServletResponse();
        new LoginCheck().doFilter(request, response, (req, res) -> reached.set(true));
        return response;
    }

    @Test
    void anonymousApiReturnsUnauthorizedJsonWithoutRedirectOrHtml() throws Exception {
        AtomicBoolean reached = new AtomicBoolean();
        MockHttpServletResponse response = call("/api/friends", null, reached);
        assertEquals(401, response.getStatus());
        assertEquals("true", response.getHeader("X-Prosta-Login-Expired"));
        assertTrue(response.getContentType().startsWith("application/json"));
        assertTrue(response.getContentAsString().contains("再度ログインしてください"));
        assertNull(response.getRedirectedUrl());
        assertFalse(reached.get());
    }

    @Test
    void expiredTabSessionAlsoReturnsUnauthorizedInsteadOfLoginPage() throws Exception {
        MockHttpSession session = new MockHttpSession();
        CustomerBean user = new CustomerBean();
        session.setAttribute("user", user);
        session.removeAttribute("user");
        AtomicBoolean reached = new AtomicBoolean();
        MockHttpServletResponse response = call("/api/login-status/heartbeat", session, reached);
        assertEquals(401, response.getStatus());
        assertEquals("true", response.getHeader("X-Prosta-Login-Expired"));
        assertNull(response.getRedirectedUrl());
        assertFalse(reached.get());
    }

    @Test
    void anonymousPageStillRedirectsToLoginError() throws Exception {
        AtomicBoolean reached = new AtomicBoolean();
        MockHttpServletResponse response = call("/friends", null, reached);
        assertEquals("/prosta/login/error", response.getRedirectedUrl());
        assertFalse(reached.get());
    }

    @Test
    void loggedInApiAndPublicLoginPageContinueNormally() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("user", new CustomerBean());
        AtomicBoolean reached = new AtomicBoolean();
        assertEquals(200, call("/api/friends", session, reached).getStatus());
        assertTrue(reached.get());
        reached.set(false);
        assertEquals(200, call("/login/error", null, reached).getStatus());
        assertTrue(reached.get());
    }
}
