package com.nanotech.flux_pro_backend.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ChainStepAssignmentRequest(
        @NotNull UUID chainStepTemplateId,
        @NotNull UUID responsibleUserId
) {
}
