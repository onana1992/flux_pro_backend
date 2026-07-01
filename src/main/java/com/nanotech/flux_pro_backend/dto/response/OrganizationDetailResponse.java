package com.nanotech.flux_pro_backend.dto.response;

import java.util.UUID;

public record OrganizationDetailResponse(
        UUID id,
        String code,
        String name,
        UUID typeId,
        String typeCode,
        String typeName,
        UUID parentId,
        String parentCode,
        boolean active
) {
}
