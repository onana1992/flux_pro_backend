package com.nanotech.flux_pro_backend.dto.response;

import java.util.UUID;

public record OrganizationTypeResponse(
        UUID id,
        String code,
        String name,
        String nameEn,
        String description,
        String color,
        int sortOrder,
        boolean allowsRoot,
        boolean isRegionalScope,
        boolean active
) {
}
