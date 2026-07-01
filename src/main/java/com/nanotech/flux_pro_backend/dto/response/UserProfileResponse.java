package com.nanotech.flux_pro_backend.dto.response;

import com.nanotech.flux_pro_backend.enumeration.UserRole;

import java.util.List;
import java.util.UUID;

public record UserProfileResponse(
        UUID id,
        String email,
        String lastName,
        String firstName,
        UserRole role,
        OrganizationSummaryResponse organization,
        boolean mustChangePassword,
        List<String> roles,
        List<String> permissions) {
}
