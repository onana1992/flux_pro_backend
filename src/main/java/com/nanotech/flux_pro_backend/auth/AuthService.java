package com.nanotech.flux_pro_backend.auth;

import com.nanotech.flux_pro_backend.auth.dto.TokenResponse;
import com.nanotech.flux_pro_backend.auth.dto.UserProfileDto;
import com.nanotech.flux_pro_backend.common.DtoMapper;
import com.nanotech.flux_pro_backend.security.JwtTokenProvider;
import com.nanotech.flux_pro_backend.security.PasswordValidator;
import com.nanotech.flux_pro_backend.security.SecurityUser;
import com.nanotech.flux_pro_backend.utilisateur.Utilisateur;
import com.nanotech.flux_pro_backend.utilisateur.UtilisateurRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
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

    private final UtilisateurRepository utilisateurRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final LoginAuditService loginAuditService;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public TokenResponse login(String email, String password, HttpServletRequest request) {
        String normalizedEmail = email.toLowerCase().trim();
        Utilisateur utilisateur = utilisateurRepository.findByEmail(normalizedEmail).orElse(null);

        if (utilisateur == null) {
            loginAuditService.log(null, normalizedEmail, false, request, "UNKNOWN_EMAIL");
            throw new BadCredentialsException("Email ou mot de passe incorrect");
        }

        if (!utilisateur.isActif()) {
            loginAuditService.log(utilisateur, normalizedEmail, false, request, "USER_INACTIVE");
            throw new BadCredentialsException("Compte inactif");
        }

        if (isLocked(utilisateur)) {
            long minutes = ChronoUnit.MINUTES.between(Instant.now(), utilisateur.getLockedUntil()) + 1;
            loginAuditService.log(utilisateur, normalizedEmail, false, request, "ACCOUNT_LOCKED");
            throw new LockedException("Compte temporairement verrouillé. Réessayez dans " + minutes + " minutes.");
        }

        if (!passwordEncoder.matches(password, utilisateur.getPasswordHash())) {
            utilisateur.setFailedLoginAttempts(utilisateur.getFailedLoginAttempts() + 1);
            if (utilisateur.getFailedLoginAttempts() >= MAX_FAILED_ATTEMPTS) {
                utilisateur.setLockedUntil(Instant.now().plus(LOCK_MINUTES, ChronoUnit.MINUTES));
            }
            utilisateurRepository.save(utilisateur);
            loginAuditService.log(utilisateur, normalizedEmail, false, request, "INVALID_PASSWORD");
            throw new BadCredentialsException("Email ou mot de passe incorrect");
        }

        utilisateur.setFailedLoginAttempts(0);
        utilisateur.setLockedUntil(null);
        utilisateurRepository.save(utilisateur);
        loginAuditService.log(utilisateur, normalizedEmail, true, request, null);

        return buildTokenResponse(utilisateur);
    }

    @Transactional
    public TokenResponse refresh(String refreshTokenValue) {
        RefreshToken refreshToken = refreshTokenRepository.findByTokenAndRevokedFalse(refreshTokenValue)
                .orElseThrow(() -> new BadCredentialsException("Refresh token invalide"));

        if (refreshToken.getExpiresAt().isBefore(Instant.now())) {
            refreshToken.setRevoked(true);
            refreshTokenRepository.save(refreshToken);
            throw new BadCredentialsException("Refresh token expiré");
        }

        Utilisateur utilisateur = refreshToken.getUtilisateur();
        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);
        return buildTokenResponse(utilisateur);
    }

    @Transactional
    public void logout(String refreshTokenValue) {
        refreshTokenRepository.findByTokenAndRevokedFalse(refreshTokenValue).ifPresent(token -> {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
        });
    }

    @Transactional
    public void changePassword(SecurityUser user, String currentPassword, String newPassword) {
        PasswordValidator.validate(newPassword);
        Utilisateur utilisateur = utilisateurRepository.findById(user.getId())
                .orElseThrow(() -> new BadCredentialsException("Utilisateur introuvable"));

        if (!passwordEncoder.matches(currentPassword, utilisateur.getPasswordHash())) {
            throw new BadCredentialsException("Mot de passe actuel incorrect");
        }

        utilisateur.setPasswordHash(passwordEncoder.encode(newPassword));
        utilisateur.setMustChangePassword(false);
        utilisateurRepository.save(utilisateur);
        refreshTokenRepository.deleteByUtilisateurId(utilisateur.getId());
    }

    private TokenResponse buildTokenResponse(Utilisateur utilisateur) {
        SecurityUser securityUser = new SecurityUser(utilisateur);
        String accessToken = jwtTokenProvider.createAccessToken(securityUser);
        String refreshValue = jwtTokenProvider.createRefreshTokenValue();

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken(refreshValue);
        refreshToken.setUtilisateur(utilisateur);
        refreshToken.setExpiresAt(Instant.now().plusMillis(jwtTokenProvider.getRefreshExpirationMs()));
        refreshTokenRepository.save(refreshToken);

        UserProfileDto profile = DtoMapper.toProfile(utilisateur);
        return new TokenResponse(
                accessToken,
                refreshValue,
                jwtTokenProvider.getAccessExpirationSeconds(),
                profile);
    }

    private boolean isLocked(Utilisateur utilisateur) {
        if (utilisateur.getLockedUntil() == null) {
            return false;
        }
        if (utilisateur.getLockedUntil().isBefore(Instant.now())) {
            utilisateur.setLockedUntil(null);
            utilisateur.setFailedLoginAttempts(0);
            utilisateurRepository.save(utilisateur);
            return false;
        }
        return true;
    }
}
