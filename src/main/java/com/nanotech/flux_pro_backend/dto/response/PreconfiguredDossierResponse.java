package com.nanotech.flux_pro_backend.dto.response;

import com.nanotech.flux_pro_backend.enumeration.PortalAudience;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record PreconfiguredDossierResponse(
        UUID id,
        String code,
        String name,
        String nameEn,
        String description,
        String fileTypeCode,
        UUID chainTemplateId,
        String chainTemplateCode,
        String chainTemplateName,
        List<PreconfiguredStepAssignmentResponse> stepAssignments,
        String directionCode,
        int sortOrder,
        boolean active,
        boolean portalEnabled,
        PortalAudience portalAudience,
        Map<String, Object> formSchema,
        List<String> requiredAttachmentKeys
) {
}
