package com.nanotech.flux_pro_backend.dto.response;

import com.nanotech.flux_pro_backend.enumeration.UserRole;

import java.util.UUID;

/** Résumé utilisateur pour listes (suppléance, etc.). */
public record UserLiteResponse(
        UUID id,
        String staffNumber,
        String email,
        String lastName,
        String firstName,
        UserRole role,
        String organizationCode,
        String organizationName,
        boolean active) {
}
