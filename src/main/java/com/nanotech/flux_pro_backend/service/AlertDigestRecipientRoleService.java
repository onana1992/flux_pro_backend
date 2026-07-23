package com.nanotech.flux_pro_backend.service;

import com.nanotech.flux_pro_backend.common.AppException;
import com.nanotech.flux_pro_backend.entity.AlertDigestRecipientRole;
import com.nanotech.flux_pro_backend.enumeration.UserRole;
import com.nanotech.flux_pro_backend.repository.AlertDigestRecipientRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AlertDigestRecipientRoleService {

    private final AlertDigestRecipientRoleRepository repository;

    @Value("${fluxpro.alerts.digest.target-role:DIRECTOR}")
    private String defaultTargetRole;

    @Transactional(readOnly = true)
    public List<UserRole> listRoles() {
        return repository.findAllByOrderByRoleAsc().stream()
                .map(AlertDigestRecipientRole::getRole)
                .toList();
    }

    /**
     * Remplace la liste complète des rôles destinataires du digest.
     * Une liste vide est autorisée (aucun digest envoyé tant qu'aucun rôle n'est configuré).
     */
    @Transactional
    public List<UserRole> replaceRoles(List<UserRole> roles) {
        Set<UserRole> unique = normalize(roles);
        repository.deleteAllInBatch();
        for (UserRole role : unique) {
            AlertDigestRecipientRole row = new AlertDigestRecipientRole();
            row.setRole(role);
            repository.save(row);
        }
        return listRoles();
    }

    @Transactional
    public List<UserRole> addRole(UserRole role) {
        if (role == null) {
            throw AppException.badRequest("DIGEST_ROLE_REQUIRED", "Role is required");
        }
        if (!repository.existsByRole(role)) {
            AlertDigestRecipientRole row = new AlertDigestRecipientRole();
            row.setRole(role);
            repository.save(row);
        }
        return listRoles();
    }

    @Transactional
    public List<UserRole> removeRole(UserRole role) {
        if (role == null) {
            throw AppException.badRequest("DIGEST_ROLE_REQUIRED", "Role is required");
        }
        repository.deleteByRole(role);
        return listRoles();
    }

    /** Seed initial si la table est vide (valeur de {@code fluxpro.alerts.digest.target-role}). */
    @Transactional
    public void seedDefaultsIfEmpty() {
        if (repository.count() > 0) {
            return;
        }
        try {
            UserRole role = UserRole.valueOf(defaultTargetRole.trim().toUpperCase());
            AlertDigestRecipientRole row = new AlertDigestRecipientRole();
            row.setRole(role);
            repository.save(row);
        } catch (IllegalArgumentException | NullPointerException e) {
            AlertDigestRecipientRole row = new AlertDigestRecipientRole();
            row.setRole(UserRole.DIRECTOR);
            repository.save(row);
        }
    }

    private Set<UserRole> normalize(List<UserRole> roles) {
        if (roles == null) {
            return Set.of();
        }
        Set<UserRole> unique = new LinkedHashSet<>();
        for (UserRole role : roles) {
            if (role != null) {
                unique.add(role);
            }
        }
        return unique;
    }
}
