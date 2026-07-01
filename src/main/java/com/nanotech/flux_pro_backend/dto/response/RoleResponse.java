package com.nanotech.flux_pro_backend.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record RoleResponse(
        UUID id,
        String name,
        String description,
        boolean systemRole,
        Instant createdAt,
        List<PermissionResponse> permissions) {
}
