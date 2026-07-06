package com.nanotech.flux_pro_backend.security;

import com.nanotech.flux_pro_backend.common.TranslatableError;
import org.springframework.security.access.AccessDeniedException;

public class AccountInactiveException extends AccessDeniedException implements TranslatableError {

    public AccountInactiveException(String message) {
        super(message);
    }

    @Override
    public String getCode() {
        return "AUTH_ACCOUNT_INACTIVE";
    }

    @Override
    public Object[] getArgs() {
        return new Object[0];
    }
}
