package com.nanotech.flux_pro_backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PassageReturnRequest(
        @NotBlank @Size(min = 20, max = 500) String reason
) {
}
