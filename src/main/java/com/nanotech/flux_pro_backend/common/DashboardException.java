package com.nanotech.flux_pro_backend.common;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class DashboardException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    public DashboardException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public static DashboardException badRequest(String code, String message) {
        return new DashboardException(HttpStatus.BAD_REQUEST, code, message);
    }

    public static DashboardException forbidden(String code, String message) {
        return new DashboardException(HttpStatus.FORBIDDEN, code, message);
    }
}
