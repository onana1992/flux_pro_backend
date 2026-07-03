package com.nanotech.flux_pro_backend.dto.response;

import com.nanotech.flux_pro_backend.enumeration.UserRole;

import java.util.UUID;

public record PassageCandidateResponse(
        UUID id,
        String firstName,
        String lastName,
        String email,
        UserRole role,
        String organizationCode,
        String organizationName
) {
}
