package com.nanotech.flux_pro_backend.dto.request;

import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record UpdateRoleRequest(
        @Size(max = 100) String name,
        String description,
        List<UUID> permissionIds) {
}
