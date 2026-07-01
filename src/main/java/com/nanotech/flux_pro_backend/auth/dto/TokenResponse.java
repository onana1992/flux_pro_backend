package com.nanotech.flux_pro_backend.auth.dto;

public record TokenResponse(
        String accessToken,
        String refreshToken,
        long expiresIn,
        UserProfileDto user
) {
}
