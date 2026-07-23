package com.nanotech.flux_pro_backend.dto.response;

import com.nanotech.flux_pro_backend.enumeration.UserRole;

import java.util.List;

public record AlertDigestRecipientRolesResponse(List<UserRole> roles) {
}
