package com.nanotech.flux_pro_backend.dto.request;

import com.nanotech.flux_pro_backend.enumeration.PortalAudience;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record PreconfiguredDossierRequest(
        @NotBlank String code,
        @NotBlank String name,
        String nameEn,
        String description,
        @NotBlank String fileTypeCode,
        UUID chainTemplateId,
        @Valid List<PreconfiguredStepAssignmentRequest> stepAssignments,
        String directionCode,
        Integer sortOrder,
        @NotNull Boolean active,
        @NotNull Boolean portalEnabled,
        PortalAudience portalAudience,
        Map<String, Object> formSchema,
        List<String> requiredAttachmentKeys
) {
}
