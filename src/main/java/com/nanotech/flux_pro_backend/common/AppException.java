package com.nanotech.flux_pro_backend.common;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Exception métier générique et traduisible, destinée à remplacer les usages ad hoc de
 * {@code IllegalArgumentException}/{@code IllegalStateException} pour signaler des règles
 * métier violées (hors domaines déjà couverts par FileException/AlertException/...).
 */
@Getter
public class AppException extends RuntimeException implements TranslatableError {

    private final HttpStatus status;
    private final String code;
    private final Object[] args;

    public AppException(HttpStatus status, String code, String message, Object... args) {
        super(message);
        this.status = status;
        this.code = code;
        this.args = args;
    }

    public AppException(HttpStatus status, String code, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
        this.code = code;
        this.args = new Object[0];
    }

    public static AppException badRequest(String code, String message, Object... args) {
        return new AppException(HttpStatus.BAD_REQUEST, code, message, args);
    }

    public static AppException conflict(String code, String message, Object... args) {
        return new AppException(HttpStatus.CONFLICT, code, message, args);
    }

    public static AppException forbidden(String code, String message, Object... args) {
        return new AppException(HttpStatus.FORBIDDEN, code, message, args);
    }

    public static AppException notFound(String code, String message, Object... args) {
        return new AppException(HttpStatus.NOT_FOUND, code, message, args);
    }

    public static AppException unauthorized(String code, String message, Object... args) {
        return new AppException(HttpStatus.UNAUTHORIZED, code, message, args);
    }

    public static AppException notImplemented(String code, String message, Object... args) {
        return new AppException(HttpStatus.NOT_IMPLEMENTED, code, message, args);
    }

    public static AppException internal(String code, String message, Object... args) {
        return new AppException(HttpStatus.INTERNAL_SERVER_ERROR, code, message, args);
    }
}
