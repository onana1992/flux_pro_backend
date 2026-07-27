package com.nanotech.flux_pro_backend.dto.response;

public record PortalAuthResponse(
        String accessToken,
        long expiresIn,
        boolean mustChangePassword,
        /** Présent quand une activation est requise (PORTAL-14). */
        String code,
        PortalUserProfileResponse user
) {
    public static PortalAuthResponse of(String token, long expiresIn, PortalUserProfileResponse user) {
        String code = user.mustChangePassword() ? "PASSWORD_CHANGE_REQUIRED" : null;
        return new PortalAuthResponse(token, expiresIn, user.mustChangePassword(), code, user);
    }
}
