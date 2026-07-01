package com.nanotech.flux_pro_backend.dto.response;

import com.nanotech.flux_pro_backend.enumeration.OrganizationType;

import java.util.List;
import java.util.UUID;

public record OrganizationTreeResponse(
        UUID id,
        String code,
        String name,
        OrganizationType type,
        boolean active,
        List<OrganizationTreeResponse> children
) {
}
