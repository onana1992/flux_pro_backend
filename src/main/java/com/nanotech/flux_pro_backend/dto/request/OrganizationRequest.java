package com.nanotech.flux_pro_backend.dto.request;

import com.nanotech.flux_pro_backend.enumeration.OrganizationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record OrganizationRequest(
        @NotBlank String code,
        @NotBlank String name,
        @NotNull OrganizationType type,
        UUID parentId,
        boolean active
) {
}
