package com.nanotech.flux_pro_backend.dto.response;

public record TenantConfigResponse(
        String tenantName,
        String productName,
        String timezone,
        String countryCode,
        String referencePrefix,
        String badge,
        String fromAddress,
        String emailRedirectTo) {
}
