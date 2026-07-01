package com.nanotech.flux_pro_backend.dto.response;

import java.util.UUID;

public record OrganizationSummaryResponse(
        UUID id,
        String code,
        String name
) {
}
