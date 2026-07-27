package com.nanotech.flux_pro_backend.dto.response;

import com.nanotech.flux_pro_backend.enumeration.UserRole;

import java.util.UUID;

public record PreconfiguredStepAssignmentResponse(
        UUID chainStepTemplateId,
        String stepLabel,
        int stepOrder,
        UserRole responsibleRole,
        UUID responsibleUserId,
        String responsibleName,
        String responsibleEmail,
        String organizationCode,
        String organizationName
) {
}
