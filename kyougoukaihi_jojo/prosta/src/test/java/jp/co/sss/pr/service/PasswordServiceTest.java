package jp.co.sss.pr.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PasswordServiceTest {

    private final PasswordService service = new PasswordService();

    @Test
    void hashesAndMatches() {
        String hash = service.encode("pass1234");
        assertFalse(service.isLegacy(hash));
        assertTrue(service.matches("pass1234", hash));
        assertFalse(service.matches("other", hash));
    }

    @Test
    void legacyPlainText() {
        assertTrue(service.isLegacy("pass1234"));
        assertTrue(service.matches("pass1234", "pass1234"));
        assertFalse(service.matches("x", "pass1234"));
    }
}
