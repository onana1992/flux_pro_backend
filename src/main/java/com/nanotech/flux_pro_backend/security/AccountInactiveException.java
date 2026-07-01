package com.nanotech.flux_pro_backend.security;

import org.springframework.security.access.AccessDeniedException;

public class AccountInactiveException extends AccessDeniedException {

    public AccountInactiveException(String message) {
        super(message);
    }
}
