package jp.co.sss.pr.login_function;

import java.io.IOException;

import org.springframework.stereotype.Component;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jp.co.sss.pr.bean.CustomerBean;

/**
 * 全リクエストに対する共通チェック。
 * 更新系リクエストの同一オリジン確認、ログイン確認、管理者専用URLの確認、セキュリティヘッダーの付与を行う。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 15)
public class LoginCheck extends HttpFilter {

    private static final Integer ADMIN = 1;

    @Override
    protected void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-Frame-Options", "SAMEORIGIN");
        response.setHeader("Referrer-Policy", "same-origin");

        if (AccessPolicy.isUnsafeMethod(request.getMethod())
                && !AccessPolicy.isSameOrigin(request.getHeader("Origin"), request.getHeader("Referer"),
                        request.getHeader("Host"))) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        String path = pathWithinApplication(request);
        if (AccessPolicy.isPublic(path)) {
            chain.doFilter(request, response);
            return;
        }

        HttpSession session = request.getSession(false);
        Object user = session == null ? null : session.getAttribute("user");
        if (user == null) {
            response.sendRedirect(request.getContextPath() + "/login/error");
            return;
        }

        if (AccessPolicy.isAdminOnly(path)
                && !(user instanceof CustomerBean bean && ADMIN.equals(bean.getPermission()))) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        chain.doFilter(request, response);
    }

    // コンテキストパスを除いた、正規化済みのパス
    private String pathWithinApplication(HttpServletRequest request) {
        String servletPath = request.getServletPath() == null ? "" : request.getServletPath();
        String pathInfo = request.getPathInfo() == null ? "" : request.getPathInfo();
        String path = servletPath + pathInfo;
        return path.isEmpty() ? "/" : path;
    }
}
