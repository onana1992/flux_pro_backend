package com.nanotech.flux_pro_backend.security;

import com.nanotech.flux_pro_backend.common.AppException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class SecurityUtils {

    public SecurityUser currentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof SecurityUser user)) {
            throw AppException.unauthorized("AUTH_NOT_AUTHENTICATED", "User not authenticated");
        }
        return user;
    }

    public SecurityUser currentUserOrNull() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof SecurityUser user) {
            return user;
        }
        return null;
    }

    public PortalSecurityUser currentPortalUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof PortalSecurityUser user)) {
            throw AppException.unauthorized("AUTH_NOT_AUTHENTICATED", "Portal user not authenticated");
        }
        return user;
    }
}
