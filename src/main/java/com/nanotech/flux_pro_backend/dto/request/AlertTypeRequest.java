package com.nanotech.flux_pro_backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AlertTypeRequest(
        @NotBlank @Size(max = 30) String code,
        @NotBlank @Size(max = 100) String label,
        String description,
        @Size(max = 100) String emailTemplateCode,
        @NotNull Boolean active) {
}
