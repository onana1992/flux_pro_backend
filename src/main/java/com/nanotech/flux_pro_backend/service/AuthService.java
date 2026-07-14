package com.nanotech.flux_pro_backend.service;

import com.nanotech.flux_pro_backend.dto.response.TokenResponse;
import com.nanotech.flux_pro_backend.dto.response.UserProfileResponse;
import com.nanotech.flux_pro_backend.entity.RefreshToken;
import com.nanotech.flux_pro_backend.entity.User;
import com.nanotech.flux_pro_backend.mapper.DtoMapper;
import com.nanotech.flux_pro_backend.repository.RefreshTokenRepository;
import com.nanotech.flux_pro_backend.repository.UserRepository;
import com.nanotech.flux_pro_backend.security.AccountInactiveException;
import com.nanotech.flux_pro_backend.security.JwtTokenProvider;
import com.nanotech.flux_pro_backend.security.PasswordValidator;
import com.nanotech.flux_pro_backend.security.RbacAuthorityService;
import com.nanotech.flux_pro_backend.security.SecurityUser;
import com.nanotech.flux_pro_backend.security.TranslatableBadCredentialsException;
import com.nanotech.flux_pro_backend.security.TranslatableLockedException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCK_MINUTES = 30;

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final LoginAuditService loginAuditService;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RbacAuthorityService rbacAuthorityService;

    @Transactional
    public TokenResponse login(String email, String password, HttpServletRequest request) {
        String normalizedEmail = email.toLowerCase().trim();
        User user = userRepository.findByEmail(normalizedEmail).orElse(null);

        if (user == null) {
            loginAuditService.log(null, normalizedEmail, false, request, "UNKNOWN_EMAIL");
            throw new TranslatableBadCredentialsException("AUTH_INVALID_CREDENTIALS", "Invalid email or password");
        }

        if (!user.isActive()) {
            loginAuditService.log(user, normalizedEmail, false, request, "USER_INACTIVE");
            throw new AccountInactiveException("Account inactive");
        }

        if (isLocked(user)) {
            long minutes = ChronoUnit.MINUTES.between(Instant.now(), user.getLockedUntil()) + 1;
            loginAuditService.log(user, normalizedEmail, false, request, "ACCOUNT_LOCKED");
            throw new TranslatableLockedException(
                    "AUTH_ACCOUNT_LOCKED_MINUTES",
                    "Account temporarily locked. Try again in " + minutes + " minutes.",
                    minutes);
        }

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            user.setFailedLoginAttempts(user.getFailedLoginAttempts() + 1);
            if (user.getFailedLoginAttempts() >= MAX_FAILED_ATTEMPTS) {
                user.setLockedUntil(Instant.now().plus(LOCK_MINUTES, ChronoUnit.MINUTES));
            }
            userRepository.save(user);
            loginAuditService.log(user, normalizedEmail, false, request, "INVALID_PASSWORD");
            throw new TranslatableBadCredentialsException("AUTH_INVALID_CREDENTIALS", "Invalid email or password");
        }

        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        userRepository.save(user);
        loginAuditService.log(user, normalizedEmail, true, request, null);

        return buildTokenResponse(user);
    }

    @Transactional
    public TokenResponse refresh(String refreshTokenValue) {
        RefreshToken refreshToken = refreshTokenRepository.findByTokenAndRevokedFalse(refreshTokenValue)
                .orElseThrow(() -> new TranslatableBadCredentialsException(
                        "AUTH_INVALID_REFRESH_TOKEN", "Invalid refresh token"));

        if (refreshToken.getExpiresAt().isBefore(Instant.now())) {
            refreshToken.setRevoked(true);
            refreshTokenRepository.save(refreshToken);
            throw new TranslatableBadCredentialsException("AUTH_REFRESH_EXPIRED", "Refresh token expired");
        }

        User user = refreshToken.getUser();
        if (!user.isActive()) {
            refreshToken.setRevoked(true);
            refreshTokenRepository.save(refreshToken);
            throw new AccountInactiveException("Account inactive");
        }

        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(Instant.now())) {
            refreshToken.setRevoked(true);
            refreshTokenRepository.save(refreshToken);
            throw new TranslatableLockedException("AUTH_ACCOUNT_LOCKED", "Account temporarily locked");
        }

        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);
        return buildTokenResponse(user);
    }

    @Transactional
    public void logout(String refreshTokenValue) {
        refreshTokenRepository.findByTokenAndRevokedFalse(refreshTokenValue).ifPresent(token -> {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
        });
    }

    /**
     * Met à jour le mot de passe, lève le flag {@code mustChangePassword}, invalide les
     * anciennes sessions et renvoie une <strong>nouvelle</strong> paire de tokens — sinon le
     * client garde un refresh token déjà révoqué et tombe en 401 juste après le changement.
     */
    @Transactional
    public TokenResponse changePassword(String currentPassword, String newPassword) {
        SecurityUser user = requireCurrentUser();
        PasswordValidator.validate(newPassword);
        User entity = userRepository.findByIdWithRolesAndOrganization(user.getId())
                .orElseThrow(() -> new TranslatableBadCredentialsException("AUTH_USER_NOT_FOUND", "User not found"));

        if (!passwordEncoder.matches(currentPassword, entity.getPasswordHash())) {
            throw new TranslatableBadCredentialsException(
                    "AUTH_CURRENT_PASSWORD_INCORRECT", "Current password is incorrect");
        }

        entity.setPasswordHash(passwordEncoder.encode(newPassword));
        entity.setMustChangePassword(false);
        entity.setFailedLoginAttempts(0);
        entity.setLockedUntil(null);
        userRepository.save(entity);
        refreshTokenRepository.deleteByUserId(entity.getId());
        return buildTokenResponse(entity);
    }

    private SecurityUser requireCurrentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof SecurityUser user)) {
            throw new TranslatableBadCredentialsException("AUTH_NOT_AUTHENTICATED", "User not authenticated");
        }
        return user;
    }

    private TokenResponse buildTokenResponse(User user) {
        User loaded = userRepository.findByIdWithRolesAndOrganization(user.getId()).orElse(user);
        RbacAuthorityService.RbacAuthorities authorities = rbacAuthorityService.resolve(loaded);
        SecurityUser securityUser = new SecurityUser(loaded, authorities);
        String accessToken = jwtTokenProvider.createAccessToken(securityUser);
        String refreshValue = jwtTokenProvider.createRefreshTokenValue();

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken(refreshValue);
        refreshToken.setUser(loaded);
        refreshToken.setExpiresAt(Instant.now().plusMillis(jwtTokenProvider.getRefreshExpirationMs()));
        refreshTokenRepository.save(refreshToken);

        UserProfileResponse profile = DtoMapper.toProfile(loaded, authorities);
        return new TokenResponse(
                accessToken,
                refreshValue,
                jwtTokenProvider.getAccessExpirationSeconds(),
                profile);
    }

    private boolean isLocked(User user) {
        if (user.getLockedUntil() == null) {
            return false;
        }
        if (user.getLockedUntil().isBefore(Instant.now())) {
            user.setLockedUntil(null);
            user.setFailedLoginAttempts(0);
            userRepository.save(user);
            return false;
        }
        return true;
    }
}
