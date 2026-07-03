package com.nanotech.flux_pro_backend.dto.request;

import com.nanotech.flux_pro_backend.enumeration.FilePriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

public record FileUpdateRequest(
        @NotBlank @Size(max = 32) String fileTypeCode,
        @NotNull UUID organizationId,
        @NotBlank @Size(max = 500) String subject,
        @NotBlank @Size(max = 255) String senderOrBeneficiary,
        @NotNull LocalDate receivedAt,
        @NotNull FilePriority priority,
        Map<String, Object> metadata
) {
}
