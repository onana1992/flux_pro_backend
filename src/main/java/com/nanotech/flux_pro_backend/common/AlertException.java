package com.nanotech.flux_pro_backend.common;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class AlertException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    public AlertException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public static AlertException badRequest(String code, String message) {
        return new AlertException(HttpStatus.BAD_REQUEST, code, message);
    }

    public static AlertException conflict(String code, String message) {
        return new AlertException(HttpStatus.CONFLICT, code, message);
    }

    public static AlertException notFound(String code, String message) {
        return new AlertException(HttpStatus.NOT_FOUND, code, message);
    }

    public static AlertException forbidden(String code, String message) {
        return new AlertException(HttpStatus.FORBIDDEN, code, message);
    }
}
