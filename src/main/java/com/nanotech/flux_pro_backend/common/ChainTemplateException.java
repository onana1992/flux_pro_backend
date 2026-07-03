package com.nanotech.flux_pro_backend.common;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class ChainTemplateException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    public ChainTemplateException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public static ChainTemplateException badRequest(String code, String message) {
        return new ChainTemplateException(HttpStatus.BAD_REQUEST, code, message);
    }

    public static ChainTemplateException forbidden(String code, String message) {
        return new ChainTemplateException(HttpStatus.FORBIDDEN, code, message);
    }

    public static ChainTemplateException conflict(String code, String message) {
        return new ChainTemplateException(HttpStatus.CONFLICT, code, message);
    }

    public static ChainTemplateException notFound(String message) {
        return new ChainTemplateException(HttpStatus.NOT_FOUND, "CHAIN_TEMPLATE_NOT_FOUND", message);
    }
}
