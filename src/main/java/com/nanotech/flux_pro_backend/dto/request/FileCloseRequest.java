package com.nanotech.flux_pro_backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record FileCloseRequest(
        @NotBlank @Size(min = 10, max = 2000) String closureReason,
        @NotNull UUID responseAttachmentId
) {
}
