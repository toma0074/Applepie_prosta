package jp.co.sss.pr.login_function;

import java.net.URI;
import java.util.List;
import java.util.Set;

/** URLごとのアクセス可否（公開・管理者専用）と、同一オリジン判定のルール。 */
public final class AccessPolicy {

    private static final Set<String> PUBLIC_EXACT = Set.of(
            "/", "/login", "/login/error", "/logout", "/regist", "/error", "/favicon.ico");

    private static final List<String> PUBLIC_PREFIX = List.of(
            "/regist/", "/css/", "/js/", "/images/", "/fonts/");

    private static final List<String> ADMIN_PREFIX = List.of(
            "/admin/", "/user/list", "/user/delete/", "/user/restore/");

    private static final Set<String> UNSAFE_METHODS = Set.of("POST", "PUT", "PATCH", "DELETE");

    private AccessPolicy() {
    }

    public static boolean isPublic(String path) {
        return PUBLIC_EXACT.contains(path) || PUBLIC_PREFIX.stream().anyMatch(path::startsWith);
    }

    public static boolean isAdminOnly(String path) {
        return ADMIN_PREFIX.stream().anyMatch(path::startsWith);
    }

    public static boolean isUnsafeMethod(String method) {
        return method != null && UNSAFE_METHODS.contains(method.toUpperCase());
    }

    /**
     * 更新系リクエストの送信元が同じサイトか。Origin、なければ Referer で判定し、
     * どちらも無い場合（ヘッダーを送らないクライアント）は許可する。
     */
    public static boolean isSameOrigin(String origin, String referer, String host) {
        String source = origin != null ? origin : referer;
        if (source == null || source.isBlank()) {
            return true;
        }
        if (host == null) {
            return false;
        }
        try {
            URI uri = URI.create(source);
            if (uri.getHost() == null) {
                return false;
            }
            String authority = uri.getPort() == -1 ? uri.getHost() : uri.getHost() + ":" + uri.getPort();
            return normalizeHost(authority).equalsIgnoreCase(normalizeHost(host));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static String normalizeHost(String hostHeader) {
        // 既定ポート表記を吸収するHost比較であり、schemeまで厳密に比較するCSRFトークン方式ではない。
        return hostHeader.replaceFirst(":(80|443)$", "");
    }
}
