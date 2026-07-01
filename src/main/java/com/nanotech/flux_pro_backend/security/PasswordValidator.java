package com.nanotech.flux_pro_backend.security;

import java.util.regex.Pattern;

public final class PasswordValidator {

    private static final Pattern POLICY = Pattern.compile(
            "^(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]).{8,}$");

    private PasswordValidator() {
    }

    public static void validate(String password) {
        if (password == null || !POLICY.matcher(password).matches()) {
            throw new IllegalArgumentException(
                    "Le mot de passe doit contenir au moins 8 caractères, une majuscule, un chiffre et un caractère spécial");
        }
    }
}
