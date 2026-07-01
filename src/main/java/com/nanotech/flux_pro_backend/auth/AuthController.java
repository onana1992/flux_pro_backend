package com.nanotech.flux_pro_backend.auth;

import com.nanotech.flux_pro_backend.auth.dto.ChangePasswordRequest;
import com.nanotech.flux_pro_backend.auth.dto.LoginRequest;
import com.nanotech.flux_pro_backend.auth.dto.RefreshTokenRequest;
import com.nanotech.flux_pro_backend.auth.dto.TokenResponse;
import com.nanotech.flux_pro_backend.security.SecurityUser;
import com.nanotech.flux_pro_backend.security.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final SecurityUtils securityUtils;

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        return authService.login(request.email(), request.password(), httpRequest);
    }

    @PostMapping("/refresh")
    public TokenResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return authService.refresh(request.refreshToken());
    }

    @PostMapping("/logout")
    public void logout(@RequestBody(required = false) RefreshTokenRequest request) {
        if (request != null && request.refreshToken() != null) {
            authService.logout(request.refreshToken());
        }
    }

    @PostMapping("/change-password")
    public void changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        SecurityUser user = securityUtils.currentUser();
        authService.changePassword(user, request.currentPassword(), request.newPassword());
    }
}
