package com.nanotech.flux_pro_backend.service.portal;

import com.nanotech.flux_pro_backend.common.AppException;
import com.nanotech.flux_pro_backend.dto.request.PortalUserCreateRequest;
import com.nanotech.flux_pro_backend.dto.request.PortalUserResetPasswordRequest;
import com.nanotech.flux_pro_backend.dto.request.PortalUserUpdateRequest;
import com.nanotech.flux_pro_backend.dto.response.PortalUserAdminResponse;
import com.nanotech.flux_pro_backend.entity.Organization;
import com.nanotech.flux_pro_backend.entity.PortalUser;
import com.nanotech.flux_pro_backend.entity.User;
import com.nanotech.flux_pro_backend.enumeration.PortalUserType;
import com.nanotech.flux_pro_backend.repository.OrganizationRepository;
import com.nanotech.flux_pro_backend.repository.PortalUserRepository;
import com.nanotech.flux_pro_backend.repository.UserRepository;
import com.nanotech.flux_pro_backend.security.PasswordValidator;
import com.nanotech.flux_pro_backend.security.SecurityUser;
import com.nanotech.flux_pro_backend.service.EmailService;
import com.nanotech.flux_pro_backend.service.TenantSettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PortalAdminUserService {

    private static final String TEMP_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@#$%";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final PortalUserRepository portalUserRepository;
    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final TenantSettingsService tenantSettingsService;

    @Transactional
    public PortalUserAdminResponse createInternal(PortalUserCreateRequest request, SecurityUser admin) {
        String email = request.email().trim().toLowerCase();
        if (portalUserRepository.existsByEmailIgnoreCase(email)) {
            throw AppException.conflict("PORTAL_EMAIL_IN_USE", "Email already registered");
        }

        String temporaryPassword = resolveTemporaryPassword(request.temporaryPassword());
        User adminUser = userRepository.findById(admin.getId())
                .orElseThrow(() -> AppException.notFound("USER_NOT_FOUND", "Admin user not found"));

        PortalUser user = new PortalUser();
        user.setEmail(email);
        user.setFirstName(request.firstName().trim());
        user.setLastName(request.lastName().trim());
        user.setPhone(blankToNull(request.phone()));
        user.setStaffNumber(blankToNull(request.staffNumber()));
        user.setPortalUserType(PortalUserType.INTERNAL_EMPLOYEE);
        user.setPasswordHash(passwordEncoder.encode(temporaryPassword));
        user.setMustChangePassword(true);
        user.setActive(true);
        user.setCreatedByAdmin(adminUser);

        if (request.organizationId() != null) {
            Organization org = organizationRepository.findById(request.organizationId())
                    .orElseThrow(() -> AppException.badRequest(
                            "ORGANIZATION_NOT_FOUND", "Organization not found"));
            user.setOrganization(org);
        }

        user = portalUserRepository.save(user);

        if (Boolean.TRUE.equals(request.sendEmail())) {
            sendCredentialsEmail(user, temporaryPassword, false);
        }

        return toAdminResponse(user, temporaryPassword);
    }

    @Transactional
    public PortalUserAdminResponse resetPassword(
            UUID id, PortalUserResetPasswordRequest request, SecurityUser admin) {
        PortalUser user = portalUserRepository.findById(id)
                .orElseThrow(() -> AppException.notFound("PORTAL_USER_NOT_FOUND", "Portal user not found"));
        if (user.getPortalUserType() != PortalUserType.INTERNAL_EMPLOYEE) {
            throw AppException.badRequest(
                    "PORTAL_RESET_INTERNAL_ONLY", "Password reset applies to internal portal users only");
        }

        String temporaryPassword = resolveTemporaryPassword(
                request != null ? request.temporaryPassword() : null);
        user.setPasswordHash(passwordEncoder.encode(temporaryPassword));
        user.setMustChangePassword(true);
        user.setPasswordChangedAt(null);
        portalUserRepository.save(user);

        if (request != null && Boolean.TRUE.equals(request.sendEmail())) {
            sendCredentialsEmail(user, temporaryPassword, true);
        }
        return toAdminResponse(user, temporaryPassword);
    }

    @Transactional(readOnly = true)
    public List<PortalUserAdminResponse> listAll() {
        return portalUserRepository.findAll().stream()
                .sorted(Comparator
                        .comparing(PortalUser::getLastName, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(PortalUser::getFirstName, String.CASE_INSENSITIVE_ORDER))
                .map(u -> toAdminResponse(u, null))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PortalUserAdminResponse> listInternal() {
        return portalUserRepository
                .findByPortalUserTypeOrderByLastNameAscFirstNameAsc(PortalUserType.INTERNAL_EMPLOYEE)
                .stream()
                .map(u -> toAdminResponse(u, null))
                .toList();
    }

    @Transactional
    public PortalUserAdminResponse setActive(UUID id, boolean active) {
        PortalUser user = portalUserRepository.findById(id)
                .orElseThrow(() -> AppException.notFound("PORTAL_USER_NOT_FOUND", "Portal user not found"));
        user.setActive(active);
        portalUserRepository.save(user);
        return toAdminResponse(user, null);
    }

    @Transactional
    public PortalUserAdminResponse update(UUID id, PortalUserUpdateRequest request) {
        PortalUser user = portalUserRepository.findById(id)
                .orElseThrow(() -> AppException.notFound("PORTAL_USER_NOT_FOUND", "Portal user not found"));
        if (user.getPortalUserType() != PortalUserType.INTERNAL_EMPLOYEE) {
            throw AppException.badRequest(
                    "PORTAL_UPDATE_INTERNAL_ONLY", "Only internal portal users can be updated here");
        }
        if (request.firstName() != null && !request.firstName().isBlank()) {
            user.setFirstName(request.firstName().trim());
        }
        if (request.lastName() != null && !request.lastName().isBlank()) {
            user.setLastName(request.lastName().trim());
        }
        if (request.phone() != null) {
            user.setPhone(blankToNull(request.phone()));
        }
        if (request.staffNumber() != null) {
            user.setStaffNumber(blankToNull(request.staffNumber()));
        }
        if (request.active() != null) {
            user.setActive(request.active());
        }
        if (request.organizationId() != null) {
            Organization org = organizationRepository.findById(request.organizationId())
                    .orElseThrow(() -> AppException.badRequest(
                            "ORGANIZATION_NOT_FOUND", "Organization not found"));
            user.setOrganization(org);
        }
        portalUserRepository.save(user);
        return toAdminResponse(user, null);
    }

    private String resolveTemporaryPassword(String provided) {
        if (provided != null && !provided.isBlank()) {
            PasswordValidator.validate(provided);
            return provided;
        }
        return generateTemporaryPassword();
    }

    private String generateTemporaryPassword() {
        // Respecte la politique : 8+, majuscule, chiffre, spécial
        List<Character> chars = new ArrayList<>();
        chars.add('A');
        chars.add('a');
        chars.add('7');
        chars.add('!');
        while (chars.size() < 12) {
            chars.add(TEMP_CHARS.charAt(RANDOM.nextInt(TEMP_CHARS.length())));
        }
        for (int i = chars.size() - 1; i > 0; i--) {
            int j = RANDOM.nextInt(i + 1);
            char tmp = chars.get(i);
            chars.set(i, chars.get(j));
            chars.set(j, tmp);
        }
        StringBuilder sb = new StringBuilder();
        chars.forEach(sb::append);
        return sb.toString();
    }

    private void sendCredentialsEmail(PortalUser user, String temporaryPassword, boolean reset) {
        String product = tenantSettingsService.productName();
        String subject = "[" + product + "] "
                + (reset ? "Réinitialisation de votre mot de passe portail"
                : "Votre compte portail interne");
        String html = "<p>Bonjour " + user.getFirstName() + ",</p>"
                + "<p>Voici vos identifiants portail :</p>"
                + "<ul><li>Email : <strong>" + user.getEmail() + "</strong></li>"
                + "<li>Mot de passe provisoire : <strong>" + temporaryPassword + "</strong></li></ul>"
                + "<p>Vous devrez le changer à la première connexion.</p>";
        emailService.sendTransactionalHtml(user.getEmail(), subject, html);
    }

    private PortalUserAdminResponse toAdminResponse(PortalUser user, String temporaryPassword) {
        return new PortalUserAdminResponse(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getPhone(),
                user.getStaffNumber(),
                user.getOrganization() != null ? user.getOrganization().getId() : null,
                user.getPortalUserType(),
                user.isMustChangePassword(),
                user.isActive(),
                temporaryPassword);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
