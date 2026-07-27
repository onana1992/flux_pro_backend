package com.nanotech.flux_pro_backend.dto.request;

public record PortalUserResetPasswordRequest(
        String temporaryPassword,
        Boolean sendEmail
) {
}
