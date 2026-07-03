package com.nanotech.flux_pro_backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PassageReasonRequest(
        @NotBlank @Size(min = 10, max = 500) String reason
) {
}
