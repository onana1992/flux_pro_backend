package com.nanotech.flux_pro_backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record OrganizationTypeRequest(
        @NotBlank String code,
        @NotBlank String name,
        String nameEn,
        String description,
        String color,
        Integer sortOrder,
        @NotNull Boolean allowsRoot,
        @NotNull Boolean isRegionalScope,
        @NotNull Boolean active
) {
}
