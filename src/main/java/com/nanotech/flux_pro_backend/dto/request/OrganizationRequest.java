package com.nanotech.flux_pro_backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record OrganizationRequest(
        @NotBlank String code,
        @NotBlank String name,
        @NotNull UUID typeId,
        UUID parentId,
        boolean active
) {
}
