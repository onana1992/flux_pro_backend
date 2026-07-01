package com.nanotech.flux_pro_backend.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdatePermissionRequest(
        @Pattern(regexp = "^[A-Z_]+:[A-Z_]+$", message = "Format attendu: RESOURCE:ACTION")
        String name,
        @Size(max = 50) String resource,
        @Size(max = 50) String action,
        String description) {
}
