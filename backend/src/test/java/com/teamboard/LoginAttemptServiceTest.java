package com.teamboard;

import com.teamboard.security.LoginAttemptService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoginAttemptServiceTest {

    @Test
    void blocksAfterFiveFailures() {
        LoginAttemptService service = new LoginAttemptService();
        for (int i = 0; i < 4; i++) {
            service.recordFailure("a@b.com");
        }
        assertFalse(service.isBlocked("a@b.com"));
        service.recordFailure("a@b.com");
        assertTrue(service.isBlocked("a@b.com"));
        assertFalse(service.isBlocked("other@b.com"));
    }

    @Test
    void successfulLoginClearsFailures() {
        LoginAttemptService service = new LoginAttemptService();
        for (int i = 0; i < 5; i++) {
            service.recordFailure("a@b.com");
        }
        service.reset("a@b.com");
        assertFalse(service.isBlocked("a@b.com"));
    }
}
