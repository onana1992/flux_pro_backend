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
import com.nanotech.flux_pro_backend.repository.UserRepository;
import com.nanotech.flux_pro_backend.security.PasswordValidator;
import com.nanotech.flux_pro_backend.security.SecurityUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private static final String TEMP_PASSWORD_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789!@#$%";

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public Page<UserResponse> search(UUID organizationId, UserRole role, String search, Pageable pageable) {
        return userRepository.search(organizationId, role, search, pageable)
                .map(DtoMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getMeProfile(SecurityUser user) {
        return DtoMapper.toProfile(findOrThrow(user.getId()));
    }

    @Transactional(readOnly = true)
    public UserResponse getById(UUID id) {
        return DtoMapper.toResponse(findOrThrow(id));
    }

    @Transactional
    public CreateUserResult create(UserRequest request) {
        validateUnique(request.staffNumber(), request.email(), null);
        Organization org = organizationRepository.findById(request.organizationId())
                .orElseThrow(() -> new IllegalArgumentException("Organization not found"));

        String tempPassword = generateTemporaryPassword();
        PasswordValidator.validate(tempPassword);

        User user = new User();
        applyRequest(user, request, org);
        user.setPasswordHash(passwordEncoder.encode(tempPassword));
        user.setMustChangePassword(true);
        userRepository.save(user);
        return new CreateUserResult(DtoMapper.toResponse(user), tempPassword);
    }

    @Transactional
    public UserResponse update(UUID id, UserRequest request) {
        User user = findOrThrow(id);
        validateUnique(request.staffNumber(), request.email(), id);
        Organization org = organizationRepository.findById(request.organizationId())
                .orElseThrow(() -> new IllegalArgumentException("Organization not found"));
        applyRequest(user, request, org);
        return DtoMapper.toResponse(userRepository.save(user));
    }

    @Transactional
    public UserResponse deactivate(UUID id) {
        User user = findOrThrow(id);
        user.setActive(false);
        return DtoMapper.toResponse(userRepository.save(user));
    }

    @Transactional
    public ResetPasswordResponse resetPassword(UUID id) {
        User user = findOrThrow(id);
        String tempPassword = generateTemporaryPassword();
        PasswordValidator.validate(tempPassword);
        user.setPasswordHash(passwordEncoder.encode(tempPassword));
        user.setMustChangePassword(true);
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        userRepository.save(user);
        return new ResetPasswordResponse(tempPassword);
    }

    @Transactional
    public ImportResult importCsv(MultipartFile file) throws IOException {
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
            } catch (Exception e) {
                errors.add("Line " + line + ": " + e.getMessage());
            }
        }
        return new ImportResult(created, updated, errors);
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
