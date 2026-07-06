package com.nanotech.flux_pro_backend.security;

import com.nanotech.flux_pro_backend.common.TranslatableError;
import org.springframework.security.authentication.BadCredentialsException;

public class TranslatableBadCredentialsException extends BadCredentialsException implements TranslatableError {

    private final String code;
    private final Object[] args;

    public TranslatableBadCredentialsException(String code, String message, Object... args) {
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
