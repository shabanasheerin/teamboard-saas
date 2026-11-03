package com.teamboard.security;

import com.teamboard.exception.ApiException;

/** Password rule: 8 to 60 characters with at least one letter and one number. */
public final class PasswordPolicy {

    private PasswordPolicy() {
    }

    public static void validate(String password) {
        boolean ok = password != null
                && password.length() >= 8
                && password.length() <= 60
                && password.matches(".*[A-Za-z].*")
                && password.matches(".*\\d.*");
        if (!ok) {
            throw ApiException.badRequest("Password must be 8 to 60 characters and include a letter and a number");
        }
    }
}
