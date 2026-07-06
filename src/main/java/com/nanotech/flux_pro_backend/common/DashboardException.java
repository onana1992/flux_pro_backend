package com.nanotech.flux_pro_backend.common;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class DashboardException extends RuntimeException implements TranslatableError {

    private final HttpStatus status;
    private final String code;
    private final Object[] args;

    public DashboardException(HttpStatus status, String code, String message, Object... args) {
        super(message);
        this.status = status;
        this.code = code;
        this.args = args;
    }

    public static DashboardException badRequest(String code, String message, Object... args) {
        return new DashboardException(HttpStatus.BAD_REQUEST, code, message, args);
    }

    public static DashboardException forbidden(String code, String message, Object... args) {
        return new DashboardException(HttpStatus.FORBIDDEN, code, message, args);
    }
}
