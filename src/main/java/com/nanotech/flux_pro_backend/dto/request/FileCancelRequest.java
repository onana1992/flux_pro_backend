package com.nanotech.flux_pro_backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FileCancelRequest(
        @NotBlank @Size(min = 10, max = 2000) String reason
) {
}
