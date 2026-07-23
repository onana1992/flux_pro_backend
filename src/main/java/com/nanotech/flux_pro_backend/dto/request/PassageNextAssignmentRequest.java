package com.nanotech.flux_pro_backend.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record PassageNextAssignmentRequest(
        @NotNull UUID passageId,
        @NotNull UUID responsibleUserId,
        List<UUID> ccUserIds
) {
}
