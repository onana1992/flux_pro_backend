package com.nanotech.flux_pro_backend.dto.request;

import jakarta.validation.constraints.Size;

import java.util.UUID;

public record PortalUserUpdateRequest(
        @Size(max = 100) String firstName,
        @Size(max = 100) String lastName,
        @Size(max = 20) String phone,
        @Size(max = 32) String staffNumber,
        UUID organizationId,
        Boolean active
) {
}
