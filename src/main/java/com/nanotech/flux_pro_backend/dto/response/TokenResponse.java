package com.nanotech.flux_pro_backend.dto.response;

public record TokenResponse(
        String accessToken,
        String refreshToken,
        long expiresIn,
        UserProfileResponse user
) {
}
