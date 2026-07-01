package com.nanotech.flux_pro_backend.security;

import com.nanotech.flux_pro_backend.entity.User;
import com.nanotech.flux_pro_backend.enumeration.UserRole;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class SecurityUser implements UserDetails {

    private final UUID id;
    private final String email;
    private final String passwordHash;
    private final UserRole role;
    private final UUID organizationId;
    private final String organizationCode;
    private final boolean active;
    private final boolean mustChangePassword;
    private final Instant lockedUntil;
    private final List<String> roleNames;
    private final List<String> permissionNames;

    public SecurityUser(User user, RbacAuthorityService.RbacAuthorities authorities) {
        this.id = user.getId();
        this.email = user.getEmail();
        this.passwordHash = user.getPasswordHash();
        this.role = user.getRole();
        this.organizationId = user.getOrganization().getId();
        this.organizationCode = user.getOrganization().getCode();
        this.active = user.isActive();
        this.mustChangePassword = user.isMustChangePassword();
        this.lockedUntil = user.getLockedUntil();
        this.roleNames = authorities.roleNames();
        this.permissionNames = authorities.permissionNames();
    }

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public UserRole getRole() {
        return role;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public String getOrganizationCode() {
        return organizationCode;
    }

    public boolean isMustChangePassword() {
        return mustChangePassword;
    }

    public List<String> getRoleNames() {
        return roleNames;
    }

    public List<String> getPermissionNames() {
        return permissionNames;
    }

    @Override
    public List<? extends GrantedAuthority> getAuthorities() {
        Set<String> authorities = new LinkedHashSet<>();
        for (String roleName : roleNames) {
            authorities.add("ROLE_" + roleName);
        }
        authorities.add("ROLE_" + role.name());
        authorities.addAll(permissionNames);
        return authorities.stream().map(SimpleGrantedAuthority::new).toList();
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
        if (lockedUntil == null) {
            return true;
        }
        return lockedUntil.isBefore(Instant.now());
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
