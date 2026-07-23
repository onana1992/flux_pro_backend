package com.nanotech.flux_pro_backend.dto.request;

import com.nanotech.flux_pro_backend.enumeration.UserRole;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record AlertDigestRecipientRolesRequest(
        @NotNull List<UserRole> roles) {
}
