package com.nanotech.flux_pro_backend.common;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class ChainTemplateException extends RuntimeException implements TranslatableError {

    private final HttpStatus status;
    private final String code;
    private final Object[] args;

    public ChainTemplateException(HttpStatus status, String code, String message, Object... args) {
        super(message);
        this.status = status;
        this.code = code;
        this.args = args;
    }

    public static ChainTemplateException badRequest(String code, String message, Object... args) {
        return new ChainTemplateException(HttpStatus.BAD_REQUEST, code, message, args);
    }

    public static ChainTemplateException forbidden(String code, String message, Object... args) {
        return new ChainTemplateException(HttpStatus.FORBIDDEN, code, message, args);
    }

    public static ChainTemplateException conflict(String code, String message, Object... args) {
        return new ChainTemplateException(HttpStatus.CONFLICT, code, message, args);
    }

    public static ChainTemplateException notFound(String code, String message, Object... args) {
        return new ChainTemplateException(HttpStatus.NOT_FOUND, code, message, args);
    }
}
