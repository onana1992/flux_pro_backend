package com.nanotech.flux_pro_backend.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record PortalOtpRequest(
        @NotBlank @Email String email
) {
}
