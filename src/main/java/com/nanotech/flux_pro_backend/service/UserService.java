package com.nanotech.flux_pro_backend.service;

import com.nanotech.flux_pro_backend.common.CsvUtils;
import com.nanotech.flux_pro_backend.common.ImportResult;
import com.nanotech.flux_pro_backend.dto.request.UserRequest;
import com.nanotech.flux_pro_backend.dto.response.ResetPasswordResponse;
import com.nanotech.flux_pro_backend.dto.response.UserProfileResponse;
import com.nanotech.flux_pro_backend.dto.response.UserResponse;
import com.nanotech.flux_pro_backend.entity.Organization;
import com.nanotech.flux_pro_backend.entity.User;
import com.nanotech.flux_pro_backend.enumeration.UserRole;
import com.nanotech.flux_pro_backend.mapper.DtoMapper;
import com.nanotech.flux_pro_backend.repository.OrganizationRepository;
import com.nanotech.flux_pro_backend.repository.RefreshTokenRepository;
import com.nanotech.flux_pro_backend.repository.UserRepository;
import com.nanotech.flux_pro_backend.security.AccessControlService;
import com.nanotech.flux_pro_backend.security.OrganizationScopeService;
import com.nanotech.flux_pro_backend.security.PasswordValidator;
import com.nanotech.flux_pro_backend.security.RbacAuthorityService;
import com.nanotech.flux_pro_backend.security.SecurityUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private static final String TEMP_PASSWORD_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789!@#$%";

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AccessControlService accessControlService;
    private final RoleService roleService;
    private final RbacAuthorityService rbacAuthorityService;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public Page<UserResponse> search(
            SecurityUser actor,
            UUID organizationId,
            UserRole role,
            String search,
            Pageable pageable) {
        if (!accessControlService.canReadUsers(actor)) {
            throw new AccessDeniedException("Access denied");
        }

        OrganizationScopeService.ScopeFilter scope = accessControlService.resolveUserSearchScope(actor);
        if (!scope.allOrganizations() && scope.organizationIds().isEmpty()) {
            return Page.empty(pageable);
        }

        if (organizationId != null && !scope.allOrganizations() && !scope.organizationIds().contains(organizationId)) {
            throw new AccessDeniedException("Access denied to this organization");
        }

        return userRepository.search(
                        scope.allOrganizations(),
                        scope.organizationIds(),
                        organizationId,
                        role,
                        search,
                        pageable)
                .map(DtoMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getMeProfile(SecurityUser user) {
        User entity = userRepository.findByIdWithRolesAndOrganization(user.getId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return DtoMapper.toProfile(entity, rbacAuthorityService.resolve(entity));
    }

    @Transactional(readOnly = true)
    public UserResponse getById(SecurityUser actor, UUID id) {
        User user = findOrThrowWithOrg(id);
        accessControlService.assertCanReadUser(actor, user);
        return DtoMapper.toResponse(user);
    }

    @Transactional
    public CreateUserResult create(SecurityUser actor, UserRequest request) {
        accessControlService.assertCanWriteUser(actor, request, null);
        validateUnique(request.staffNumber(), request.email(), null);
        Organization org = organizationRepository.findById(request.organizationId())
                .orElseThrow(() -> new IllegalArgumentException("Organization not found"));

        String tempPassword = resolveTemporaryPassword(request.temporaryPassword());
        PasswordValidator.validate(tempPassword);

        User user = new User();
        applyRequest(user, request, org);
        user.setPasswordHash(passwordEncoder.encode(tempPassword));
        user.setMustChangePassword(true);
        userRepository.save(user);
        roleService.syncPrimaryRole(user);
        userRepository.save(user);
        return new CreateUserResult(DtoMapper.toResponse(user), tempPassword);
    }

    @Transactional
    public UserResponse update(SecurityUser actor, UUID id, UserRequest request) {
        User user = findOrThrowWithOrg(id);
        accessControlService.assertCanWriteUser(actor, request, user);
        validateUnique(request.staffNumber(), request.email(), id);
        Organization org = organizationRepository.findById(request.organizationId())
                .orElseThrow(() -> new IllegalArgumentException("Organization not found"));
        applyRequest(user, request, org);
        roleService.syncPrimaryRole(user);
        return DtoMapper.toResponse(userRepository.save(user));
    }

    @Transactional
    public UserResponse deactivate(SecurityUser actor, UUID id) {
        User user = findOrThrowWithOrg(id);
        accessControlService.assertCanManageUser(actor, user);
        user.setActive(false);
        refreshTokenRepository.revokeAllByUserId(id);
        return DtoMapper.toResponse(userRepository.save(user));
    }

    @Transactional
    public UserResponse activate(SecurityUser actor, UUID id) {
        User user = findOrThrowWithOrg(id);
        accessControlService.assertCanManageUser(actor, user);
        if (!user.getOrganization().isActive()) {
            throw new IllegalArgumentException("Organization is inactive");
        }
        user.setActive(true);
        return DtoMapper.toResponse(userRepository.save(user));
    }

    @Transactional
    public UserResponse unlock(SecurityUser actor, UUID id) {
        User user = findOrThrowWithOrg(id);
        accessControlService.assertCanManageUser(actor, user);
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        return DtoMapper.toResponse(userRepository.save(user));
    }

    @Transactional
    public ResetPasswordResponse resetPassword(SecurityUser actor, UUID id) {
        User user = findOrThrowWithOrg(id);
        accessControlService.assertCanManageUser(actor, user);
        if (actor.getRole() != UserRole.SUPER_ADMIN) {
            throw new AccessDeniedException("Access denied");
        }
        String tempPassword = generateTemporaryPassword();
        PasswordValidator.validate(tempPassword);
        user.setPasswordHash(passwordEncoder.encode(tempPassword));
        user.setMustChangePassword(true);
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        refreshTokenRepository.revokeAllByUserId(id);
        userRepository.save(user);
        return new ResetPasswordResponse(tempPassword);
    }

    @Transactional
    public ImportResult importCsv(SecurityUser actor, MultipartFile file) throws IOException {
        if (actor.getRole() != UserRole.SUPER_ADMIN) {
            throw new AccessDeniedException("Access denied");
        }
        List<Map<String, String>> rows = CsvUtils.parseSemicolonCsv(file.getInputStream());
        int created = 0;
        int updated = 0;
        List<String> errors = new ArrayList<>();
        String defaultPassword = "ChangeMe@MINTP1";

        for (int i = 0; i < rows.size(); i++) {
            Map<String, String> row = rows.get(i);
            int line = i + 2;
            try {
                String email = row.get("email").toLowerCase().trim();
                String staffNumber = row.get("matricule");
                var existing = userRepository.findByEmail(email);
                User user = existing.orElseGet(User::new);
                boolean isNew = existing.isEmpty();

                Organization org = organizationRepository.findByCode(row.get("organisation_code"))
                        .orElseThrow(() -> new IllegalArgumentException("Organization " + row.get("organisation_code")));

                user.setStaffNumber(staffNumber);
                user.setEmail(email);
                user.setLastName(row.get("nom"));
                user.setFirstName(row.get("prenom"));
                user.setPhone(row.getOrDefault("telephone", null));
                user.setRole(UserRole.valueOf(row.get("role")));
                user.setOrganization(org);
                user.setJobTitle(row.getOrDefault("fonction", row.getOrDefault("service", "")));
                user.setActive(Boolean.parseBoolean(row.getOrDefault("actif", "true")));

                if (isNew) {
                    user.setPasswordHash(passwordEncoder.encode(defaultPassword));
                    user.setMustChangePassword(true);
                    created++;
                } else {
                    updated++;
                }
                userRepository.save(user);
                roleService.syncPrimaryRole(user);
                userRepository.save(user);
            } catch (Exception e) {
                errors.add("Line " + line + ": " + e.getMessage());
            }
        }
        return new ImportResult(created, updated, errors);
    }

    @Transactional(readOnly = true)
    public Set<UUID> assignableOrganizationIds(SecurityUser actor) {
        OrganizationScopeService.ScopeFilter scope = accessControlService.resolveUserSearchScope(actor);
        if (scope.allOrganizations()) {
            return Set.of();
        }
        return scope.organizationIds();
    }

    @Transactional(readOnly = true)
    public void assertCanManageUser(SecurityUser actor, UUID userId) {
        accessControlService.assertCanManageUser(actor, findOrThrowWithOrg(userId));
    }

    private void applyRequest(User user, UserRequest request, Organization org) {
        user.setStaffNumber(request.staffNumber());
        user.setEmail(request.email().toLowerCase().trim());
        user.setLastName(request.lastName());
        user.setFirstName(request.firstName());
        user.setPhone(request.phone());
        user.setRole(request.role());
        user.setOrganization(org);
        user.setJobTitle(request.jobTitle());
        user.setActive(request.active());
    }

    private void validateUnique(String staffNumber, String email, UUID excludeId) {
        userRepository.findByStaffNumber(staffNumber).ifPresent(u -> {
            if (excludeId == null || !u.getId().equals(excludeId)) {
                throw new IllegalArgumentException("Staff number already in use");
            }
        });
        userRepository.findByEmail(email.toLowerCase().trim()).ifPresent(u -> {
            if (excludeId == null || !u.getId().equals(excludeId)) {
                throw new IllegalArgumentException("Email already in use");
            }
        });
    }

    private User findOrThrow(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    private User findOrThrowWithOrg(UUID id) {
        return userRepository.findByIdWithOrganization(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    private String resolveTemporaryPassword(String provided) {
        if (provided != null && !provided.isBlank()) {
            return provided.trim();
        }
        return generateTemporaryPassword();
    }

    private String generateTemporaryPassword() {
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder();
        sb.append("Mintp@");
        for (int i = 0; i < 6; i++) {
            sb.append(TEMP_PASSWORD_CHARS.charAt(random.nextInt(TEMP_PASSWORD_CHARS.length())));
        }
        return sb.toString();
    }

    public record CreateUserResult(UserResponse user, String temporaryPassword) {
    }
}
