package com.nanotech.flux_pro_backend.auth.dto;

import com.nanotech.flux_pro_backend.security.UserRole;

import java.util.UUID;

public record UserProfileDto(
        UUID id,
        String email,
        String nom,
        String prenom,
        UserRole role,
        OrganisationSummaryDto organisation,
        boolean mustChangePassword
) {
}
