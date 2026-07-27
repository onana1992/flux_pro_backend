package com.nanotech.flux_pro_backend.dto.response;

import com.nanotech.flux_pro_backend.enumeration.PortalAudience;

import java.util.List;
import java.util.Map;

public record PortalFormSchemaResponse(
        String code,
        String name,
        PortalAudience portalAudience,
        Map<String, Object> formSchema,
        List<String> requiredAttachmentKeys
) {
}
