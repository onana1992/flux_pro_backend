package com.nanotech.flux_pro_backend.dto.response;

public record PortalOtpSentResponse(
        String message,
        String email,
        int expiresInSeconds
) {
}
