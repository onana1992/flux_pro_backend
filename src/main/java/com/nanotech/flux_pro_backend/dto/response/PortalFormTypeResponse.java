package com.nanotech.flux_pro_backend.dto.response;

import com.nanotech.flux_pro_backend.enumeration.PortalAudience;

import java.util.List;
import java.util.UUID;

public record PortalFormTypeResponse(
        String code,
        String name,
        String nameEn,
        String description,
        PortalAudience portalAudience,
        List<String> requiredAttachmentKeys
) {
}
