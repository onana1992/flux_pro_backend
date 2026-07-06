package com.nanotech.flux_pro_backend.common;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class FileException extends RuntimeException implements TranslatableError {

    private final HttpStatus status;
    private final String code;
    private final Object[] args;

    public FileException(HttpStatus status, String code, String message, Object... args) {
        super(message);
        this.status = status;
        this.code = code;
        this.args = args;
    }

    public static FileException badRequest(String code, String message, Object... args) {
        return new FileException(HttpStatus.BAD_REQUEST, code, message, args);
    }

    public static FileException forbidden(String code, String message, Object... args) {
        return new FileException(HttpStatus.FORBIDDEN, code, message, args);
    }

    public static FileException conflict(String code, String message, Object... args) {
        return new FileException(HttpStatus.CONFLICT, code, message, args);
    }

    public static FileException notFound(String code, String message, Object... args) {
        return new FileException(HttpStatus.NOT_FOUND, code, message, args);
    }
}
