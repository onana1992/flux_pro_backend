package com.nanotech.flux_pro_backend.dto.request;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

public record AssignPermissionsRequest(@NotEmpty List<UUID> permissionIds) {
}
