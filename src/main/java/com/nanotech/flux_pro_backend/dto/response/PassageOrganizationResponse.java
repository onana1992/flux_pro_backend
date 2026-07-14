package com.nanotech.flux_pro_backend.dto.response;

import java.util.UUID;

public record PassageOrganizationResponse(
        UUID id,
        String code,
        String name,
        int depth
) {
}
