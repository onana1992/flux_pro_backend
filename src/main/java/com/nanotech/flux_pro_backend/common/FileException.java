package com.nanotech.flux_pro_backend.common;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class FileException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    public FileException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public static FileException badRequest(String code, String message) {
        return new FileException(HttpStatus.BAD_REQUEST, code, message);
    }

    public static FileException forbidden(String message) {
        return new FileException(HttpStatus.FORBIDDEN, "FILE_ACCESS_DENIED", message);
    }

    public static FileException conflict(String code, String message) {
        return new FileException(HttpStatus.CONFLICT, code, message);
    }

    public static FileException notFound(String message) {
        return new FileException(HttpStatus.NOT_FOUND, "FILE_NOT_FOUND", message);
    }
}
