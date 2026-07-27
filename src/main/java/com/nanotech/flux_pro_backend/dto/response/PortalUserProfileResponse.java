package com.nanotech.flux_pro_backend.dto.response;

import com.nanotech.flux_pro_backend.enumeration.PortalUserType;

import java.util.UUID;

public record PortalUserProfileResponse(
        UUID id,
        String email,
        String firstName,
        String lastName,
        String phone,
        PortalUserType portalUserType,
        boolean mustChangePassword,
        boolean emailVerified,
        boolean active
) {
}
