package com.nanotech.flux_pro_backend.utilisateur;

import com.nanotech.flux_pro_backend.common.ImportResult;
import com.nanotech.flux_pro_backend.security.SecurityUtils;
import com.nanotech.flux_pro_backend.security.UserRole;
import com.nanotech.flux_pro_backend.utilisateur.dto.ResetPasswordResponse;
import com.nanotech.flux_pro_backend.utilisateur.dto.UtilisateurRequest;
import com.nanotech.flux_pro_backend.utilisateur.dto.UtilisateurResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/utilisateurs")
@RequiredArgsConstructor
public class UtilisateurController {

    private final UtilisateurService utilisateurService;
    private final SecurityUtils securityUtils;

    @GetMapping("/me")
    public UtilisateurResponse me() {
        return utilisateurService.getMe(securityUtils.currentUser());
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN_METIER', 'DIRECTEUR')")
    public Page<UtilisateurResponse> search(
            @RequestParam(required = false) UUID organisationId,
            @RequestParam(required = false) UserRole role,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20) Pageable pageable) {
        return utilisateurService.search(organisationId, role, search, pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN_METIER', 'DIRECTEUR')")
    public UtilisateurResponse getById(@PathVariable UUID id) {
        return utilisateurService.getById(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN_METIER')")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> create(@Valid @RequestBody UtilisateurRequest request) {
        var result = utilisateurService.create(request);
        return Map.of(
                "user", result.user(),
                "temporaryPassword", result.temporaryPassword());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN_METIER')")
    public UtilisateurResponse update(@PathVariable UUID id, @Valid @RequestBody UtilisateurRequest request) {
        return utilisateurService.update(id, request);
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN_METIER')")
    public UtilisateurResponse deactivate(@PathVariable UUID id) {
        return utilisateurService.deactivate(id);
    }

    @PostMapping("/{id}/reset-password")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResetPasswordResponse resetPassword(@PathVariable UUID id) {
        return utilisateurService.resetPassword(id);
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ImportResult importCsv(@RequestParam("file") MultipartFile file) throws IOException {
        return utilisateurService.importCsv(file);
    }
}
