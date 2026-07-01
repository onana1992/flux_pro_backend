package com.nanotech.flux_pro_backend.dto.response;

import com.nanotech.flux_pro_backend.enumeration.UserRole;

import java.util.List;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String staffNumber,
        String email,
        String lastName,
        String firstName,
        String phone,
        UserRole role,
        OrganizationSummaryResponse organization,
        String jobTitle,
        boolean active,
        boolean mustChangePassword,
        List<RoleSummaryResponse> roles) {
}
