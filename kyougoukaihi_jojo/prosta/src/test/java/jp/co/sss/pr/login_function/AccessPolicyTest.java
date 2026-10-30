package jp.co.sss.pr.login_function;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AccessPolicyTest {

    @Test
    void publicPaths() {
        assertTrue(AccessPolicy.isPublic("/login"));
        assertTrue(AccessPolicy.isPublic("/css/a.css"));
        assertFalse(AccessPolicy.isPublic("/mypage"));
        assertFalse(AccessPolicy.isPublic("/user/list"));
    }

    @Test
    void adminPaths() {
        assertTrue(AccessPolicy.isAdminOnly("/admin/questions"));
        assertTrue(AccessPolicy.isAdminOnly("/user/list"));
        assertFalse(AccessPolicy.isAdminOnly("/mypage"));
    }

    @Test
    void sameOrigin() {
        assertTrue(AccessPolicy.isSameOrigin("http://localhost:7778", null, "localhost:7778"));
        assertFalse(AccessPolicy.isSameOrigin("http://evil.example", null, "localhost:7778"));
        assertTrue(AccessPolicy.isSameOrigin(null, null, "localhost:7778"));
    }
}
