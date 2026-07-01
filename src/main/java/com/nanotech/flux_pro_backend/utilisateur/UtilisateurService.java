package com.nanotech.flux_pro_backend.utilisateur;

import com.nanotech.flux_pro_backend.common.CsvUtils;
import com.nanotech.flux_pro_backend.common.DtoMapper;
import com.nanotech.flux_pro_backend.common.ImportResult;
import com.nanotech.flux_pro_backend.organisation.Organisation;
import com.nanotech.flux_pro_backend.organisation.OrganisationRepository;
import com.nanotech.flux_pro_backend.security.PasswordValidator;
import com.nanotech.flux_pro_backend.security.SecurityUser;
import com.nanotech.flux_pro_backend.security.UserRole;
import com.nanotech.flux_pro_backend.utilisateur.dto.ResetPasswordResponse;
import com.nanotech.flux_pro_backend.utilisateur.dto.UtilisateurRequest;
import com.nanotech.flux_pro_backend.utilisateur.dto.UtilisateurResponse;
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
public class UtilisateurService {

    private static final String TEMP_PASSWORD_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789!@#$%";

    private final UtilisateurRepository utilisateurRepository;
    private final OrganisationRepository organisationRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public Page<UtilisateurResponse> search(UUID organisationId, UserRole role, String search, Pageable pageable) {
        return utilisateurRepository.search(organisationId, role, search, pageable)
                .map(DtoMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public UtilisateurResponse getById(UUID id) {
        return DtoMapper.toResponse(findOrThrow(id));
    }

    @Transactional(readOnly = true)
    public UtilisateurResponse getMe(SecurityUser user) {
        return getById(user.getId());
    }

    @Transactional
    public CreateUserResult create(UtilisateurRequest request) {
        validateUnique(request.matricule(), request.email(), null);
        Organisation org = organisationRepository.findById(request.organisationId())
                .orElseThrow(() -> new IllegalArgumentException("Organisation introuvable"));

        String tempPassword = generateTemporaryPassword();
        PasswordValidator.validate(tempPassword);

        Utilisateur u = new Utilisateur();
        applyRequest(u, request, org);
        u.setPasswordHash(passwordEncoder.encode(tempPassword));
        u.setMustChangePassword(true);
        utilisateurRepository.save(u);
        return new CreateUserResult(DtoMapper.toResponse(u), tempPassword);
    }

    @Transactional
    public UtilisateurResponse update(UUID id, UtilisateurRequest request) {
        Utilisateur u = findOrThrow(id);
        validateUnique(request.matricule(), request.email(), id);
        Organisation org = organisationRepository.findById(request.organisationId())
                .orElseThrow(() -> new IllegalArgumentException("Organisation introuvable"));
        applyRequest(u, request, org);
        return DtoMapper.toResponse(utilisateurRepository.save(u));
    }

    @Transactional
    public UtilisateurResponse deactivate(UUID id) {
        Utilisateur u = findOrThrow(id);
        u.setActif(false);
        return DtoMapper.toResponse(utilisateurRepository.save(u));
    }

    @Transactional
    public ResetPasswordResponse resetPassword(UUID id) {
        Utilisateur u = findOrThrow(id);
        String tempPassword = generateTemporaryPassword();
        PasswordValidator.validate(tempPassword);
        u.setPasswordHash(passwordEncoder.encode(tempPassword));
        u.setMustChangePassword(true);
        u.setFailedLoginAttempts(0);
        u.setLockedUntil(null);
        utilisateurRepository.save(u);
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
                String matricule = row.get("matricule");
                var existing = utilisateurRepository.findByEmail(email);
                Utilisateur u = existing.orElseGet(Utilisateur::new);
                boolean isNew = existing.isEmpty();

                Organisation org = organisationRepository.findByCode(row.get("organisation_code"))
                        .orElseThrow(() -> new IllegalArgumentException("Organisation " + row.get("organisation_code")));

                u.setMatricule(matricule);
                u.setEmail(email);
                u.setNom(row.get("nom"));
                u.setPrenom(row.get("prenom"));
                u.setTelephone(row.getOrDefault("telephone", null));
                u.setRole(UserRole.valueOf(row.get("role")));
                u.setOrganisation(org);
                u.setFonction(row.getOrDefault("fonction", row.getOrDefault("service", "")));
                u.setActif(Boolean.parseBoolean(row.getOrDefault("actif", "true")));

                if (isNew) {
                    u.setPasswordHash(passwordEncoder.encode(defaultPassword));
                    u.setMustChangePassword(true);
                    created++;
                } else {
                    updated++;
                }
                utilisateurRepository.save(u);
            } catch (Exception e) {
                errors.add("Ligne " + line + ": " + e.getMessage());
            }
        }
        return new ImportResult(created, updated, errors);
    }

    private void applyRequest(Utilisateur u, UtilisateurRequest request, Organisation org) {
        u.setMatricule(request.matricule());
        u.setEmail(request.email().toLowerCase().trim());
        u.setNom(request.nom());
        u.setPrenom(request.prenom());
        u.setTelephone(request.telephone());
        u.setRole(request.role());
        u.setOrganisation(org);
        u.setFonction(request.fonction());
        u.setActif(request.actif());
    }

    private void validateUnique(String matricule, String email, UUID excludeId) {
        utilisateurRepository.findByMatricule(matricule).ifPresent(u -> {
            if (excludeId == null || !u.getId().equals(excludeId)) {
                throw new IllegalArgumentException("Matricule déjà utilisé");
            }
        });
        utilisateurRepository.findByEmail(email.toLowerCase().trim()).ifPresent(u -> {
            if (excludeId == null || !u.getId().equals(excludeId)) {
                throw new IllegalArgumentException("Email déjà utilisé");
            }
        });
    }

    private Utilisateur findOrThrow(UUID id) {
        return utilisateurRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable"));
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

    public record CreateUserResult(UtilisateurResponse user, String temporaryPassword) {
    }
}
