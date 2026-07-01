package com.nanotech.flux_pro_backend.dto.response;

import java.util.List;
import java.util.UUID;

public record OrganizationTreeResponse(
        UUID id,
        String code,
        String name,
        OrganizationTypeResponse type,
        boolean active,
        List<OrganizationTreeResponse> children
) {
}
