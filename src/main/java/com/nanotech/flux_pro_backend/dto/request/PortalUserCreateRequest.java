package com.nanotech.flux_pro_backend.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record PortalUserCreateRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(max = 100) String firstName,
        @NotBlank @Size(max = 100) String lastName,
        @Size(max = 20) String phone,
        @Size(max = 32) String staffNumber,
        UUID organizationId,
        /** Mot de passe provisoire ; si absent, généré côté serveur. */
        String temporaryPassword,
        Boolean sendEmail
) {
}
