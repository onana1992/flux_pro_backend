package com.nanotech.flux_pro_backend.security;

import com.nanotech.flux_pro_backend.entity.PortalUser;
import com.nanotech.flux_pro_backend.enumeration.PortalUserType;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Principal Spring Security pour le realm portail (distinct de {@link SecurityUser} métier).
 */
public class PortalSecurityUser implements UserDetails {

    private final UUID id;
    private final String email;
    private final String passwordHash;
    private final PortalUserType portalUserType;
    private final boolean active;
    private final boolean mustChangePassword;
    private final boolean emailVerified;

    public PortalSecurityUser(PortalUser user) {
        this.id = user.getId();
        this.email = user.getEmail();
        this.passwordHash = user.getPasswordHash();
        this.portalUserType = user.getPortalUserType();
        this.active = user.isActive();
        this.mustChangePassword = user.isMustChangePassword();
        this.emailVerified = user.getEmailVerifiedAt() != null;
    }

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public PortalUserType getPortalUserType() {
        return portalUserType;
    }

    public boolean isMustChangePassword() {
        return mustChangePassword;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(
                new SimpleGrantedAuthority("ROLE_PORTAL"),
                new SimpleGrantedAuthority("ROLE_PORTAL_" + portalUserType.name()));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return active;
    }
}
