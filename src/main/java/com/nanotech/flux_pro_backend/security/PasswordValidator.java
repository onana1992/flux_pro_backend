package com.nanotech.flux_pro_backend.security;

import com.nanotech.flux_pro_backend.common.AppException;

import java.util.regex.Pattern;

public final class PasswordValidator {

    private static final Pattern POLICY = Pattern.compile(
            "^(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]).{8,}$");

    private PasswordValidator() {
    }

    public static void validate(String password) {
        if (password == null || !POLICY.matcher(password).matches()) {
            throw AppException.badRequest(
                    "AUTH_PASSWORD_POLICY",
                    "Password must be at least 8 characters long and contain an uppercase letter, "
                            + "a digit and a special character");
        }
    }
}
