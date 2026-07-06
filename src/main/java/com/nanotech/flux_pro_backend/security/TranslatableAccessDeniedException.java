package com.nanotech.flux_pro_backend.security;

import com.nanotech.flux_pro_backend.common.TranslatableError;
import org.springframework.security.access.AccessDeniedException;

/**
 * Variante traduisible de {@link AccessDeniedException} : conserve la sémantique Spring
 * Security (interceptée comme telle par le filtre de sécurité si nécessaire) tout en portant
 * un code i18n + des arguments pour la traduction du message côté GlobalExceptionHandler.
 */
public class TranslatableAccessDeniedException extends AccessDeniedException implements TranslatableError {

    private final String code;
    private final Object[] args;

    public TranslatableAccessDeniedException(String code, String message, Object... args) {
        super(message);
        this.code = code;
        this.args = args;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public Object[] getArgs() {
        return args;
    }
}
