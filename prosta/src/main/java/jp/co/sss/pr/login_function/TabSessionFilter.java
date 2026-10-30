package jp.co.sss.pr.login_function;

import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.HtmlUtils;
import org.springframework.web.util.UriComponentsBuilder;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import jakarta.servlet.http.HttpSession;

/**
 * Cookieのセッションはブラウザー単位の入れ物とし、ログイン情報はタブ別の論理セッションに分ける。
 * 認証フィルターより先にHttpSessionを差し替え、同じタブ内のリクエストだけを直列化する。
 */
public class TabSessionFilter extends OncePerRequestFilter {
    public static final String PARAMETER = "_tab";
    private static final String REGISTRY = TabSessionFilter.class.getName();

    @Configuration(proxyBeanMethods = false)
    public static class Registration {
        @Bean
        FilterRegistrationBean<TabSessionFilter> tabSessionFilter() {
            FilterRegistrationBean<TabSessionFilter> registration =
                    new FilterRegistrationBean<>(new TabSessionFilter());
            registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
            return registration;
        }
    }

    private static boolean asset(String path) {
        return path.startsWith("/css/") || path.startsWith("/js/")
                || path.startsWith("/images/") || path.startsWith("/fonts/")
                || path.equals("/favicon.ico");
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return asset(request.getRequestURI().substring(request.getContextPath().length()));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {
        response.setHeader("Cache-Control", "no-store");
        String token = request.getParameter(PARAMETER);
        String header = request.getHeader("X-Prosta-Tab");
        if (token == null) token = header;
        if (token == null) {
            if (!"GET".equals(request.getMethod())
                    || "XMLHttpRequest".equals(request.getHeader("X-Requested-With"))
                    || request.getRequestURI().startsWith(request.getContextPath() + "/api/")) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Tab session is required");
                return;
            }
            response.setContentType("text/html;charset=UTF-8");
            response.getWriter().write("<!DOCTYPE html><html><head><meta charset=\"UTF-8\">"
                    + "<script src=\"" + HtmlUtils.htmlEscape(request.getContextPath())
                    + "/js/tab-session.js\" data-bootstrap=\"true\"></script></head><body></body></html>");
            return;
        }
        String[] parameters = request.getParameterValues(PARAMETER);
        if (!token.matches("[0-9a-f]{8}(-[0-9a-f]{4}){3}-[0-9a-f]{12}")
                || (header != null && !header.equals(token))
                || (parameters != null && parameters.length != 1)) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid tab session");
            return;
        }

        HttpSession browser = request.getSession();
        Map<String, Tab> tabs = registry(browser);
        long now = System.currentTimeMillis();
        tabs.entrySet().removeIf(entry -> entry.getValue().expired(now, browser.getMaxInactiveInterval()));
        Tab tab = tabs.computeIfAbsent(token, key -> new Tab(key, browser));
        // 採点・画面描画・後続フィルターのDB確定まで直列化し、同じ解答の二重保存を防ぐ。
        synchronized (tab) {
            if (!token.equals(tab.id) || tabs.get(token) != tab) {
                response.sendError(HttpServletResponse.SC_CONFLICT, "Tab session changed; reload this page");
                return;
            }
            tab.lastAccess = System.currentTimeMillis();
            if (tab.session != null) tab.session.fresh = false;
            if (request.getRequestURI().equals(request.getContextPath() + "/api/tab-navigation")
                    && "GET".equals(request.getMethod())) {
                String ticket = UUID.randomUUID().toString();
                tab.tickets.clear();
                tab.tickets.add(ticket);
                response.setContentType("application/json");
                response.getWriter().write("{\"ticket\":\"" + ticket + "\"}");
                return;
            }
            // 複製タブの判定前に、テスト開始など状態を変える画面GETを実行させない。
            // _navはタブにひも付く単回チケットであり、ログイン資格情報ではない。
            if ("GET".equals(request.getMethod())
                    && "document".equals(request.getHeader("Sec-Fetch-Dest"))
                    && !tab.tickets.remove(request.getParameter("_nav"))) {
                response.setContentType("text/html;charset=UTF-8");
                response.getWriter().write("<!DOCTYPE html><html><head><meta charset=\"UTF-8\">"
                        + "<script src=\"" + HtmlUtils.htmlEscape(request.getContextPath())
                        + "/js/tab-session.js\" data-bootstrap=\"true\"></script></head><body></body></html>");
                return;
            }
            TabRequest wrappedRequest = new TabRequest(request, tab, tabs);
            chain.doFilter(wrappedRequest, new TabResponse(response, wrappedRequest));
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Tab> registry(HttpSession browser) {
        synchronized (browser) {
            Object existing = browser.getAttribute(REGISTRY);
            if (existing == null) {
                existing = new ConcurrentHashMap<String, Tab>();
                browser.setAttribute(REGISTRY, existing);
            }
            return (Map<String, Tab>) existing;
        }
    }

    private static final class Tab {
        private String id;
        private final HttpSession browser;
        private final Set<String> tickets = new HashSet<>();
        private volatile Session session;
        private volatile long lastAccess = System.currentTimeMillis();

        Tab(String id, HttpSession browser) {
            this.id = id;
            this.browser = browser;
        }

        boolean expired(long now, int defaultTimeout) {
            int timeout = session == null ? defaultTimeout : session.getMaxInactiveInterval();
            return timeout > 0 && now - lastAccess > timeout * 1000L;
        }
    }

    private static final class TabRequest extends HttpServletRequestWrapper {
        private final Tab tab;
        private final Map<String, Tab> tabs;

        TabRequest(HttpServletRequest request, Tab tab, Map<String, Tab> tabs) {
            super(request);
            this.tab = tab;
            this.tabs = tabs;
        }

        @Override
        public HttpSession getSession(boolean create) {
            if (tab.session == null && create) tab.session = new Session(tab);
            return tab.session;
        }

        @Override
        public HttpSession getSession() {
            return getSession(true);
        }

        @Override
        public String changeSessionId() {
            if (tab.session == null) throw new IllegalStateException("No tab session");
            tabs.remove(tab.id, tab);
            tab.id = UUID.randomUUID().toString();
            tabs.put(tab.id, tab);
            // Login starts a new identity, so no previous answers/passwords survive it.
            tab.session.attributes.clear();
            return tab.id;
        }
    }

    private static final class TabResponse extends HttpServletResponseWrapper {
        private final TabRequest request;

        TabResponse(HttpServletResponse response, TabRequest request) {
            super(response);
            this.request = request;
        }

        private String withTab(String value) {
            if (value == null || value.startsWith("#")) return value;
            URI resolved = URI.create(request.getRequestURL().toString()).resolve(value);
            URI origin = URI.create(request.getRequestURL().toString());
            if (!origin.getScheme().equalsIgnoreCase(resolved.getScheme())
                    || !origin.getRawAuthority().equalsIgnoreCase(resolved.getRawAuthority())) return value;
            String context = request.getContextPath();
            String path = resolved.getPath();
            if (!(path.equals(context) || path.startsWith(context + "/"))
                    || asset(path.substring(context.length()))
                    || path.startsWith(context + "/api/")) return value;
            return UriComponentsBuilder.fromUriString(value)
                    .replaceQueryParam(PARAMETER, request.tab.id).build(true).toUriString();
        }

        @Override
        public String encodeURL(String url) {
            return withTab(url);
        }

        @Override
        public String encodeRedirectURL(String url) {
            return withTab(url);
        }

        @Override
        public void sendRedirect(String location) throws IOException {
            super.sendRedirect(withTab(location));
        }
    }

    private static final class Session implements HttpSession {
        private final Tab tab;
        private final long created = System.currentTimeMillis();
        private final Map<String, Object> attributes = new HashMap<>();
        private volatile int timeout;
        private boolean valid = true;
        private boolean fresh = true;

        Session(Tab tab) {
            this.tab = tab;
            timeout = tab.browser.getMaxInactiveInterval();
        }

        private void checkValid() {
            if (!valid) throw new IllegalStateException("Tab session is invalid");
        }

        @Override public long getCreationTime() { checkValid(); return created; }
        @Override public String getId() { return tab.id; }
        @Override public long getLastAccessedTime() { checkValid(); return tab.lastAccess; }
        @Override public ServletContext getServletContext() { return tab.browser.getServletContext(); }
        @Override public void setMaxInactiveInterval(int interval) { timeout = interval; }
        @Override public int getMaxInactiveInterval() { return timeout; }
        @Override public Object getAttribute(String name) { checkValid(); return attributes.get(name); }
        @Override public Enumeration<String> getAttributeNames() {
            checkValid();
            return Collections.enumeration(attributes.keySet());
        }
        @Override public void setAttribute(String name, Object value) {
            checkValid();
            if (value == null) attributes.remove(name);
            else attributes.put(name, value);
        }
        @Override public void removeAttribute(String name) { checkValid(); attributes.remove(name); }
        @Override public boolean isNew() { checkValid(); return fresh; }
        @Override public void invalidate() {
            checkValid();
            valid = false;
            attributes.clear();
            tab.session = null;
        }
    }
}
