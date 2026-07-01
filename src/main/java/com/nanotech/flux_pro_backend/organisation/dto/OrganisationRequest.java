package com.nanotech.flux_pro_backend.organisation.dto;

import com.nanotech.flux_pro_backend.organisation.OrganisationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record OrganisationRequest(
        @NotBlank String code,
        @NotBlank String nom,
        @NotNull OrganisationType type,
        UUID parentId,
        boolean actif
) {
}
