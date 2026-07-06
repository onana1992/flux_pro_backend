package com.nanotech.flux_pro_backend.security;

import com.nanotech.flux_pro_backend.common.AppException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PasswordValidatorTest {

    @Test
    void acceptsValidPassword() {
        assertDoesNotThrow(() -> PasswordValidator.validate("Mintp@2025"));
    }

    @Test
    void rejectsWeakPassword() {
        assertThrows(AppException.class, () -> PasswordValidator.validate("password"));
    }
}
