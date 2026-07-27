package com.nanotech.flux_pro_backend.controller;

import com.nanotech.flux_pro_backend.dto.request.PreconfiguredDossierRequest;
import com.nanotech.flux_pro_backend.dto.response.PreconfiguredDossierResponse;
import com.nanotech.flux_pro_backend.entity.PreconfiguredDossier;
import com.nanotech.flux_pro_backend.entity.User;
import com.nanotech.flux_pro_backend.mapper.PreconfiguredDossierMapper;
import com.nanotech.flux_pro_backend.repository.UserRepository;
import com.nanotech.flux_pro_backend.security.RbacPermissions;
import com.nanotech.flux_pro_backend.security.RequiresPermission;
import com.nanotech.flux_pro_backend.service.PreconfiguredDossierService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class PreconfiguredDossierController {

    private final PreconfiguredDossierService preconfiguredDossierService;
    private final UserRepository userRepository;

    @GetMapping("/api/admin/preconfigured-dossiers")
    @RequiresPermission(RbacPermissions.FILE_TYPES_READ)
    public List<PreconfiguredDossierResponse> listAll() {
        List<PreconfiguredDossier> dossiers = preconfiguredDossierService.listAll();
        Map<UUID, User> users = loadUsers(dossiers);
        return dossiers.stream()
                .map(d -> PreconfiguredDossierMapper.toResponse(d, users))
                .toList();
    }

    @GetMapping("/api/admin/preconfigured-dossiers/{id}")
    @RequiresPermission(RbacPermissions.FILE_TYPES_READ)
    public PreconfiguredDossierResponse getById(@PathVariable UUID id) {
        PreconfiguredDossier d = preconfiguredDossierService.getById(id);
        return PreconfiguredDossierMapper.toResponse(d, loadUsers(List.of(d)));
    }

    @GetMapping("/api/admin/preconfigured-dossiers/by-code/{code}")
    @RequiresPermission(RbacPermissions.FILE_TYPES_READ)
    public PreconfiguredDossierResponse getByCode(@PathVariable String code) {
        PreconfiguredDossier d = preconfiguredDossierService.getByCode(code);
        return PreconfiguredDossierMapper.toResponse(d, loadUsers(List.of(d)));
    }

    @PostMapping("/api/admin/preconfigured-dossiers")
    @RequiresPermission(RbacPermissions.FILE_TYPES_CREATE)
    @ResponseStatus(HttpStatus.CREATED)
    public PreconfiguredDossierResponse create(@Valid @RequestBody PreconfiguredDossierRequest request) {
        PreconfiguredDossier d = preconfiguredDossierService.create(request);
        d = preconfiguredDossierService.getById(d.getId());
        return PreconfiguredDossierMapper.toResponse(d, loadUsers(List.of(d)));
    }

    @PutMapping("/api/admin/preconfigured-dossiers/{id}")
    @RequiresPermission(RbacPermissions.FILE_TYPES_UPDATE)
    public PreconfiguredDossierResponse update(
            @PathVariable UUID id,
            @Valid @RequestBody PreconfiguredDossierRequest request) {
        preconfiguredDossierService.update(id, request);
        PreconfiguredDossier d = preconfiguredDossierService.getById(id);
        return PreconfiguredDossierMapper.toResponse(d, loadUsers(List.of(d)));
    }

    @PatchMapping("/api/admin/preconfigured-dossiers/{id}/deactivate")
    @RequiresPermission(RbacPermissions.FILE_TYPES_UPDATE)
    public PreconfiguredDossierResponse deactivate(@PathVariable UUID id) {
        PreconfiguredDossier d = preconfiguredDossierService.deactivate(id);
        d = preconfiguredDossierService.getById(d.getId());
        return PreconfiguredDossierMapper.toResponse(d, loadUsers(List.of(d)));
    }

    @DeleteMapping("/api/admin/preconfigured-dossiers/{id}")
    @RequiresPermission(RbacPermissions.FILE_TYPES_DELETE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        preconfiguredDossierService.delete(id);
    }

    private Map<UUID, User> loadUsers(List<PreconfiguredDossier> dossiers) {
        Set<UUID> ids = new HashSet<>();
        for (PreconfiguredDossier d : dossiers) {
            ids.addAll(PreconfiguredDossierService.parseStepAssignments(d.getStepAssignments()).values());
        }
        if (ids.isEmpty()) {
            return Map.of();
        }
        Map<UUID, User> byId = new HashMap<>();
        for (User u : userRepository.findAllByIdWithOrganization(ids)) {
            byId.put(u.getId(), u);
        }
        return byId;
    }
}
