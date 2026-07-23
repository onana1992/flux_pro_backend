package com.nanotech.flux_pro_backend.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TenantSettingsRequest(
        @NotBlank @Size(max = 150) String tenantName,
        @NotBlank @Size(max = 80) String productName,
        @NotBlank @Size(max = 64) String timezone,
        @NotBlank @Size(min = 2, max = 2) String countryCode,
        @NotBlank @Size(max = 20) String referencePrefix,
        @NotBlank @Size(max = 200) String badge,
        @NotBlank @Email @Size(max = 200) String fromAddress,
        @Size(max = 200) String emailRedirectTo) {
}
