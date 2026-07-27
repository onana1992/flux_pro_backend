package com.nanotech.flux_pro_backend.dto.response;

import com.nanotech.flux_pro_backend.enumeration.PortalUserType;

import java.util.UUID;

public record PortalUserAdminResponse(
        UUID id,
        String email,
        String firstName,
        String lastName,
        String phone,
        String staffNumber,
        UUID organizationId,
        PortalUserType portalUserType,
        boolean mustChangePassword,
        boolean active,
        /** Présent une seule fois à la création / reset. */
        String temporaryPassword
) {
}
