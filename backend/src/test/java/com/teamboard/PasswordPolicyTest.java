package com.teamboard;

import com.teamboard.exception.ApiException;
import com.teamboard.security.PasswordPolicy;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PasswordPolicyTest {

    @Test
    void acceptsStrongPassword() {
        assertDoesNotThrow(() -> PasswordPolicy.validate("Welcome2026"));
    }

    @Test
    void rejectsWeakPasswords() {
        assertThrows(ApiException.class, () -> PasswordPolicy.validate("abc12"));
        assertThrows(ApiException.class, () -> PasswordPolicy.validate("onlyletters"));
        assertThrows(ApiException.class, () -> PasswordPolicy.validate("12345678"));
        assertThrows(ApiException.class, () -> PasswordPolicy.validate(null));
    }
}
