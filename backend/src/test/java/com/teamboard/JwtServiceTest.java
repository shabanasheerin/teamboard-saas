package com.teamboard;

import com.teamboard.entity.Role;
import com.teamboard.security.AuthUser;
import com.teamboard.security.JwtService;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtServiceTest {

    private final JwtService jwtService = new JwtService("test-secret-key-test-secret-key-test-secret-key", 60_000);
    private final AuthUser user = new AuthUser(1L, 10L, "manager@acme.com", "Priya", Role.MANAGER, true, false);

    @Test
    void tokenContainsEmailAndRole() {
        String token = jwtService.generateAccessToken(user);
        assertEquals("manager@acme.com", jwtService.parse(token).getSubject());
        assertEquals("MANAGER", jwtService.parse(token).get("role", String.class));
    }

    @Test
    void tamperedTokenIsRejected() {
        String token = jwtService.generateAccessToken(user) + "x";
        assertThrows(JwtException.class, () -> jwtService.parse(token));
    }

    @Test
    void shortSecretIsRejectedAtStartup() {
        assertThrows(IllegalStateException.class, () -> new JwtService("too-short", 60_000));
    }
}
