package com.nanotech.flux_pro_backend.dto.request;

import com.nanotech.flux_pro_backend.enumeration.FilePriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record PortalSubmissionCreateRequest(
        @NotBlank String preconfiguredDossierCode,
        String subject,
        @NotNull Map<String, Object> formData,
        FilePriority priority,
        /** Si true (défaut), soumet immédiatement après création (PJ requises doivent déjà être présentes). */
        Boolean submit
) {
}
