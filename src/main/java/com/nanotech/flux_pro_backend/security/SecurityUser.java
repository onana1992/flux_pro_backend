package com.nanotech.flux_pro_backend.security;

import com.nanotech.flux_pro_backend.utilisateur.Utilisateur;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Getter
public class SecurityUser implements UserDetails {

    private final UUID id;
    private final String email;
    private final String passwordHash;
    private final UserRole role;
    private final UUID organisationId;
    private final String organisationCode;
    private final boolean actif;

    public SecurityUser(Utilisateur utilisateur) {
        this.id = utilisateur.getId();
        this.email = utilisateur.getEmail();
        this.passwordHash = utilisateur.getPasswordHash();
        this.role = utilisateur.getRole();
        this.organisationId = utilisateur.getOrganisation().getId();
        this.organisationCode = utilisateur.getOrganisation().getCode();
        this.actif = utilisateur.isActif();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
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
        return actif;
    }
}
