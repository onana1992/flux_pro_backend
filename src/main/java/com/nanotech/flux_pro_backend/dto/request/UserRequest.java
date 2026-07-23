package com.nanotech.flux_pro_backend.dto.request;

import com.nanotech.flux_pro_backend.enumeration.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record UserRequest(
        @NotBlank String staffNumber,
        @NotBlank @Email String email,
        @NotBlank String lastName,
        @NotBlank String firstName,
        String phone,
        @NotNull UserRole role,
        @NotNull UUID organizationId,
        String jobTitle,
        boolean active,
        boolean organizationHead,
        UUID substituteId,
        String temporaryPassword
) {
}
