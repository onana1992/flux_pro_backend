package com.nanotech.flux_pro_backend.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record PassageReassignRequest(
        @NotNull UUID responsibleUserId
) {
}
