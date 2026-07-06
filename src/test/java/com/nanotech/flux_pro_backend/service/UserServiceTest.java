package com.nanotech.flux_pro_backend.service;

import com.nanotech.flux_pro_backend.security.PasswordValidator;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class UserServiceTest {

    /**
     * Le mot de passe temporaire généré doit toujours respecter la politique appliquée par
     * {@link PasswordValidator} (cf. AUTH_PASSWORD_POLICY) : une ancienne implémentation
     * (préfixe fixe "Mintp@" + 6 caractères aléatoires) ne garantissait pas statistiquement la
     * présence d'un chiffre et pouvait donc être rejetée à la création/réinitialisation d'un
     * utilisateur. On répète le test pour couvrir l'aléatoire du générateur.
     */
    @RepeatedTest(200)
    void generateTemporaryPassword_alwaysMatchesPasswordPolicy() {
        String password = UserService.generateTemporaryPassword();

        assertThatCode(() -> PasswordValidator.validate(password)).doesNotThrowAnyException();
        assertThat(password).hasSize(12);
        assertThat(password).matches(".*[A-Z].*");
        assertThat(password).matches(".*\\d.*");
        assertThat(password).matches(".*[!@#$%].*");
    }

    @Test
    void generateTemporaryPassword_generatesDistinctValues() {
        String first = UserService.generateTemporaryPassword();
        String second = UserService.generateTemporaryPassword();

        assertThat(first).isNotEqualTo(second);
    }
}
