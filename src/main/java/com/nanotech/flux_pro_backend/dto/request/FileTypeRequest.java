package com.nanotech.flux_pro_backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record FileTypeRequest(
        @NotBlank @Size(max = 32) String code,
        @NotBlank @Size(max = 255) String name,
        @Size(max = 255) String nameEn,
        String description,
        @Size(max = 32) String directionCode,
        Integer sortOrder,
        @NotNull Boolean active) {
}
