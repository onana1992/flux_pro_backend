package com.nanotech.flux_pro_backend.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PermissionResponse(
        UUID id,
        String name,
        String resource,
        String action,
        String description,
        Instant createdAt) {
}
