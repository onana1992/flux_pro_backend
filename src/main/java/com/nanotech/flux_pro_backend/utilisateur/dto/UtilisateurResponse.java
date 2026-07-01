package com.nanotech.flux_pro_backend.utilisateur.dto;

import com.nanotech.flux_pro_backend.auth.dto.OrganisationSummaryDto;
import com.nanotech.flux_pro_backend.security.UserRole;

import java.util.UUID;

public record UtilisateurResponse(
        UUID id,
        String matricule,
        String email,
        String nom,
        String prenom,
        String telephone,
        UserRole role,
        OrganisationSummaryDto organisation,
        String fonction,
        boolean actif,
        boolean mustChangePassword
) {
}
