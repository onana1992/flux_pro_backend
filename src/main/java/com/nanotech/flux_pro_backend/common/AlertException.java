package com.nanotech.flux_pro_backend.common;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class AlertException extends RuntimeException implements TranslatableError {

    private final HttpStatus status;
    private final String code;
    private final Object[] args;

    public AlertException(HttpStatus status, String code, String message, Object... args) {
        super(message);
        this.status = status;
        this.code = code;
        this.args = args;
    }

    public static AlertException badRequest(String code, String message, Object... args) {
        return new AlertException(HttpStatus.BAD_REQUEST, code, message, args);
    }

    public static AlertException conflict(String code, String message, Object... args) {
        return new AlertException(HttpStatus.CONFLICT, code, message, args);
    }

    public static AlertException notFound(String code, String message, Object... args) {
        return new AlertException(HttpStatus.NOT_FOUND, code, message, args);
    }

    public static AlertException forbidden(String code, String message, Object... args) {
        return new AlertException(HttpStatus.FORBIDDEN, code, message, args);
    }
}
