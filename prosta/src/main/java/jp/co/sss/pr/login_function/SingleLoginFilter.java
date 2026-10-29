package jp.co.sss.pr.login_function;

import java.io.IOException;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jp.co.sss.pr.bean.CustomerBean;
import jp.co.sss.pr.entity.Customer;

/**
 * 認証済み要求を顧客行ロック下で処理し、最後に成功したログインだけを有効にする。
 * ロックは照合直後に解放せず、更新・ログアウト・コミットまで保持して失効後の書込みを防ぐ。
 */
public class SingleLoginFilter extends OncePerRequestFilter {
    public static final String LOGIN_TOKEN = "activeLoginToken";
    private final EntityManager entityManager;
    private final TransactionTemplate transaction;

    public SingleLoginFilter(EntityManager entityManager, PlatformTransactionManager transactions) {
        this.entityManager = entityManager;
        this.transaction = new TransactionTemplate(transactions);
    }

    @Configuration(proxyBeanMethods = false)
    public static class Registration {
        @Bean
        FilterRegistrationBean<SingleLoginFilter> singleLoginFilter(
                EntityManager entityManager, PlatformTransactionManager transactions) {
            var registration = new FilterRegistrationBean<>(new SingleLoginFilter(entityManager, transactions));
            registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 20);
            return registration;
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return AccessPolicy.isPublic(path) && !path.equals("/logout");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session == null || !(session.getAttribute("user") instanceof CustomerBean user)) {
            chain.doFilter(request, response);
            return;
        }
        try {
            // MVCで解決された4xxでも、内側の@Transactionalがrollback-onlyにした場合はここで失敗する。
            // 例外を一律に握りつぶさず、読取専用API側で想定内エラーの扱いを限定して定義する。
            transaction.executeWithoutResult(status -> {
                Customer current = entityManager.find(Customer.class, user.getUserId(),
                        LockModeType.PESSIMISTIC_WRITE);
                // OpenEntityManagerInViewで先に取得済みでも、ロック待ちの間に更新されたトークンを再読込する。
                if (current != null) entityManager.refresh(current, LockModeType.PESSIMISTIC_WRITE);
                Object token = session.getAttribute(LOGIN_TOKEN);
                try {
                    if (!(token instanceof String) || current == null
                            || Boolean.TRUE.equals(current.getDeleteFlag())
                            || !token.equals(current.getActiveLoginToken())) {
                        session.invalidate();
                        response.setHeader("X-Prosta-Login-Expired", "true");
                        if (request.getRequestURI().startsWith(request.getContextPath() + "/api/")) {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        } else {
                            response.sendRedirect(request.getContextPath() + "/login/error");
                        }
                        return;
                    }
                    chain.doFilter(request, response);
                } catch (IOException | ServletException e) {
                    throw new RequestFailure(e);
                }
            });
        } catch (RequestFailure e) {
            if (e.getCause() instanceof IOException io) throw io;
            throw (ServletException) e.getCause();
        }
    }

    private static final class RequestFailure extends RuntimeException {
        RequestFailure(Exception cause) { super(cause); }
    }
}
