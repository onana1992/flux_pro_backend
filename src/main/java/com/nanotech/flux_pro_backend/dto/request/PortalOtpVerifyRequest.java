package com.nanotech.flux_pro_backend.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PortalOtpVerifyRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 4, max = 10) String code
) {
}
