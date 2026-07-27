package com.nanotech.flux_pro_backend.service.portal;

import com.nanotech.flux_pro_backend.common.AppException;
import com.nanotech.flux_pro_backend.dto.request.PortalActivateRequest;
import com.nanotech.flux_pro_backend.dto.request.PortalLoginRequest;
import com.nanotech.flux_pro_backend.dto.request.PortalOtpRequest;
import com.nanotech.flux_pro_backend.dto.request.PortalOtpVerifyRequest;
import com.nanotech.flux_pro_backend.dto.request.PortalRegisterRequest;
import com.nanotech.flux_pro_backend.dto.response.PortalAuthResponse;
import com.nanotech.flux_pro_backend.dto.response.PortalOtpSentResponse;
import com.nanotech.flux_pro_backend.dto.response.PortalUserProfileResponse;
import com.nanotech.flux_pro_backend.entity.PortalOtpChallenge;
import com.nanotech.flux_pro_backend.entity.PortalUser;
import com.nanotech.flux_pro_backend.enumeration.PortalOtpPurpose;
import com.nanotech.flux_pro_backend.enumeration.PortalUserType;
import com.nanotech.flux_pro_backend.repository.PortalOtpChallengeRepository;
import com.nanotech.flux_pro_backend.repository.PortalUserRepository;
import com.nanotech.flux_pro_backend.security.JwtTokenProvider;
import com.nanotech.flux_pro_backend.security.PasswordValidator;
import com.nanotech.flux_pro_backend.security.PortalAuthRateLimiter;
import com.nanotech.flux_pro_backend.security.PortalSecurityUser;
import com.nanotech.flux_pro_backend.service.ClockService;
import com.nanotech.flux_pro_backend.service.EmailService;
import com.nanotech.flux_pro_backend.service.TenantSettingsService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class PortalAuthService {

    private static final int MAX_OTP_ATTEMPTS = 5;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final PortalUserRepository portalUserRepository;
    private final PortalOtpChallengeRepository otpChallengeRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final PortalAuthRateLimiter rateLimiter;
    private final EmailService emailService;
    private final TenantSettingsService tenantSettingsService;
    private final ClockService clockService;

    @Value("${fluxpro.portal.otp.ttl-seconds:600}")
    private int otpTtlSeconds;

    @Value("${fluxpro.portal.otp.length:6}")
    private int otpLength;

    @Transactional
    public PortalAuthResponse login(PortalLoginRequest request, HttpServletRequest httpRequest) {
        String email = normalizeEmail(request.email());
        rateLimiter.checkLogin(clientKey(httpRequest, email));

        PortalUser user = portalUserRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> AppException.unauthorized(
                        "PORTAL_INVALID_CREDENTIALS", "Invalid email or password"));

        if (user.getPortalUserType() != PortalUserType.INTERNAL_EMPLOYEE) {
            throw AppException.badRequest(
                    "PORTAL_LOGIN_USE_OTP",
                    "External users must authenticate with OTP");
        }
        if (!user.isActive()) {
            throw AppException.forbidden("PORTAL_USER_INACTIVE", "Portal account is inactive");
        }
        if (user.getPasswordHash() == null
                || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw AppException.unauthorized(
                    "PORTAL_INVALID_CREDENTIALS", "Invalid email or password");
        }
        return issueTokens(user);
    }

    @Transactional
    public PortalAuthResponse activate(PortalActivateRequest request, HttpServletRequest httpRequest) {
        String email = normalizeEmail(request.email());
        rateLimiter.checkActivate(clientKey(httpRequest, email));

        PortalUser user = portalUserRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> AppException.unauthorized(
                        "PORTAL_INVALID_CREDENTIALS", "Invalid email or password"));

        if (user.getPortalUserType() != PortalUserType.INTERNAL_EMPLOYEE) {
            throw AppException.badRequest(
                    "PORTAL_ACTIVATE_INTERNAL_ONLY", "Only internal portal users can activate a password");
        }
        if (!user.isActive()) {
            throw AppException.forbidden("PORTAL_USER_INACTIVE", "Portal account is inactive");
        }
        if (user.getPasswordHash() == null
                || !passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw AppException.unauthorized(
                    "PORTAL_INVALID_CREDENTIALS", "Invalid email or password");
        }

        PasswordValidator.validate(request.newPassword());
        if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
            throw AppException.badRequest(
                    "PORTAL_PASSWORD_SAME", "New password must be different from the temporary password");
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setMustChangePassword(false);
        user.setPasswordChangedAt(clockService.now());
        if (user.getEmailVerifiedAt() == null) {
            user.setEmailVerifiedAt(clockService.now());
        }
        portalUserRepository.save(user);
        return issueTokens(user);
    }

    @Transactional
    public PortalOtpSentResponse register(PortalRegisterRequest request, HttpServletRequest httpRequest) {
        String email = normalizeEmail(request.email());
        rateLimiter.checkOtp(clientKey(httpRequest, email));

        if (portalUserRepository.existsByEmailIgnoreCase(email)) {
            throw AppException.conflict("PORTAL_EMAIL_IN_USE", "Email already registered");
        }

        PortalUser user = new PortalUser();
        user.setEmail(email);
        user.setFirstName(request.firstName().trim());
        user.setLastName(request.lastName().trim());
        user.setPhone(blankToNull(request.phone()));
        user.setPortalUserType(PortalUserType.EXTERNAL);
        user.setActive(true);
        user.setMustChangePassword(false);
        user.setPasswordHash(null);
        // organisationName stocké en métadonnée légère dans phone/job? Spec has optional org —
        // keep on user as phone only for MVP; ignore organizationName persistence for now
        // or append to nothing. Could store in staffNumber unused for external - skip.
        portalUserRepository.save(user);

        return sendOtp(email, PortalOtpPurpose.REGISTER);
    }

    @Transactional
    public PortalOtpSentResponse requestOtp(PortalOtpRequest request, HttpServletRequest httpRequest) {
        String email = normalizeEmail(request.email());
        rateLimiter.checkOtp(clientKey(httpRequest, email));

        PortalUser user = portalUserRepository.findByEmailIgnoreCase(email).orElse(null);
        // Anti-énumération : réponse générique si inconnu
        if (user == null) {
            return new PortalOtpSentResponse(
                    "If the account exists, an OTP has been sent",
                    email,
                    otpTtlSeconds);
        }
        if (user.getPortalUserType() != PortalUserType.EXTERNAL) {
            throw AppException.badRequest(
                    "PORTAL_OTP_EXTERNAL_ONLY", "OTP authentication is for external users only");
        }
        if (!user.isActive()) {
            throw AppException.forbidden("PORTAL_USER_INACTIVE", "Portal account is inactive");
        }

        PortalOtpPurpose purpose = user.getEmailVerifiedAt() == null
                ? PortalOtpPurpose.REGISTER
                : PortalOtpPurpose.LOGIN;
        return sendOtp(email, purpose);
    }

    @Transactional
    public PortalAuthResponse verifyOtp(PortalOtpVerifyRequest request, HttpServletRequest httpRequest) {
        String email = normalizeEmail(request.email());
        rateLimiter.checkLogin(clientKey(httpRequest, email));

        PortalUser user = portalUserRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> AppException.unauthorized(
                        "PORTAL_OTP_INVALID", "Invalid or expired OTP"));

        if (user.getPortalUserType() != PortalUserType.EXTERNAL) {
            throw AppException.badRequest(
                    "PORTAL_OTP_EXTERNAL_ONLY", "OTP authentication is for external users only");
        }
        if (!user.isActive()) {
            throw AppException.forbidden("PORTAL_USER_INACTIVE", "Portal account is inactive");
        }

        PortalOtpChallenge challenge = otpChallengeRepository
                .findFirstByEmailIgnoreCaseAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc(
                        email, PortalOtpPurpose.LOGIN)
                .or(() -> otpChallengeRepository
                        .findFirstByEmailIgnoreCaseAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc(
                                email, PortalOtpPurpose.REGISTER))
                .orElseThrow(() -> AppException.unauthorized(
                        "PORTAL_OTP_INVALID", "Invalid or expired OTP"));

        Instant now = clockService.now();
        if (challenge.isExpired(now)) {
            challenge.setConsumedAt(now);
            otpChallengeRepository.save(challenge);
            throw AppException.unauthorized("PORTAL_OTP_EXPIRED", "OTP has expired");
        }
        if (challenge.getAttemptCount() >= MAX_OTP_ATTEMPTS) {
            challenge.setConsumedAt(now);
            otpChallengeRepository.save(challenge);
            throw AppException.unauthorized("PORTAL_OTP_LOCKED", "Too many invalid OTP attempts");
        }

        if (!passwordEncoder.matches(request.code().trim(), challenge.getCodeHash())) {
            challenge.setAttemptCount(challenge.getAttemptCount() + 1);
            otpChallengeRepository.save(challenge);
            throw AppException.unauthorized("PORTAL_OTP_INVALID", "Invalid or expired OTP");
        }

        challenge.setConsumedAt(now);
        otpChallengeRepository.save(challenge);
        otpChallengeRepository.consumeAllOpen(email, PortalOtpPurpose.LOGIN, now);
        otpChallengeRepository.consumeAllOpen(email, PortalOtpPurpose.REGISTER, now);

        if (user.getEmailVerifiedAt() == null) {
            user.setEmailVerifiedAt(now);
            portalUserRepository.save(user);
        }
        return issueTokens(user);
    }

    private PortalOtpSentResponse sendOtp(String email, PortalOtpPurpose purpose) {
        Instant now = clockService.now();
        otpChallengeRepository.consumeAllOpen(email, purpose, now);

        String code = generateOtpCode();
        PortalOtpChallenge challenge = new PortalOtpChallenge();
        challenge.setEmail(email);
        challenge.setPurpose(purpose);
        challenge.setCodeHash(passwordEncoder.encode(code));
        challenge.setExpiresAt(now.plus(otpTtlSeconds, ChronoUnit.SECONDS));
        otpChallengeRepository.save(challenge);

        String product = tenantSettingsService.productName();
        String subject = "[" + product + "] Code de vérification portail";
        String html = "<p>Votre code OTP FluxPro Portail :</p>"
                + "<p style=\"font-size:24px;font-weight:bold;letter-spacing:4px\">" + code + "</p>"
                + "<p>Valable " + (otpTtlSeconds / 60) + " minutes. Ne le partagez pas.</p>";
        emailService.sendTransactionalHtml(email, subject, html);

        return new PortalOtpSentResponse("OTP sent", email, otpTtlSeconds);
    }

    private PortalAuthResponse issueTokens(PortalUser user) {
        PortalSecurityUser securityUser = new PortalSecurityUser(user);
        String token = jwtTokenProvider.createPortalAccessToken(securityUser);
        return PortalAuthResponse.of(
                token,
                jwtTokenProvider.getAccessExpirationSeconds(),
                toProfile(user));
    }

    public static PortalUserProfileResponse toProfile(PortalUser user) {
        return new PortalUserProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getPhone(),
                user.getPortalUserType(),
                user.isMustChangePassword(),
                user.getEmailVerifiedAt() != null,
                user.isActive());
    }

    private String generateOtpCode() {
        int bound = (int) Math.pow(10, otpLength);
        int min = bound / 10;
        int value = min + RANDOM.nextInt(bound - min);
        return String.valueOf(value);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String clientKey(HttpServletRequest request, String email) {
        String ip = request != null ? request.getRemoteAddr() : "unknown";
        return ip + "|" + email;
    }
}
