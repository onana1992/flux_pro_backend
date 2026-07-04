package com.nanotech.flux_pro_backend.controller;

import com.nanotech.flux_pro_backend.dto.request.AlertTypeRequest;
import com.nanotech.flux_pro_backend.dto.response.AlertTypeResponse;
import com.nanotech.flux_pro_backend.mapper.AlertTypeMapper;
import com.nanotech.flux_pro_backend.security.RbacPermissions;
import com.nanotech.flux_pro_backend.security.RequiresPermission;
import com.nanotech.flux_pro_backend.service.AlertTypeService;
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

import java.util.List;
import java.util.UUID;

/** Catalogue administrable des types d'alerte (ALR-F17) — jamais une énumération figée. */
@RestController
@RequiredArgsConstructor
public class AlertTypeController {

    private final AlertTypeService alertTypeService;

    /** Liste active — utilisée par les écrans de configuration des règles d'alerte. */
    @GetMapping("/api/alert-types")
    public List<AlertTypeResponse> listActive() {
        return alertTypeService.listActive().stream().map(AlertTypeMapper::toResponse).toList();
    }

    @GetMapping("/api/admin/alert-types")
    @RequiresPermission(RbacPermissions.ALERT_TYPES_READ)
    public List<AlertTypeResponse> listAll() {
        return alertTypeService.listAll().stream().map(AlertTypeMapper::toResponse).toList();
    }

    @GetMapping("/api/admin/alert-types/{id}")
    @RequiresPermission(RbacPermissions.ALERT_TYPES_READ)
    public AlertTypeResponse getById(@PathVariable UUID id) {
        return AlertTypeMapper.toResponse(alertTypeService.getById(id));
    }

    @PostMapping("/api/admin/alert-types")
    @RequiresPermission(RbacPermissions.ALERT_TYPES_CREATE)
    @ResponseStatus(HttpStatus.CREATED)
    public AlertTypeResponse create(@Valid @RequestBody AlertTypeRequest request) {
        return AlertTypeMapper.toResponse(alertTypeService.create(request));
    }

    @PutMapping("/api/admin/alert-types/{id}")
    @RequiresPermission(RbacPermissions.ALERT_TYPES_UPDATE)
    public AlertTypeResponse update(@PathVariable UUID id, @Valid @RequestBody AlertTypeRequest request) {
        return AlertTypeMapper.toResponse(alertTypeService.update(id, request));
    }

    @PatchMapping("/api/admin/alert-types/{id}/activate")
    @RequiresPermission(RbacPermissions.ALERT_TYPES_UPDATE)
    public AlertTypeResponse activate(@PathVariable UUID id) {
        return AlertTypeMapper.toResponse(alertTypeService.activate(id));
    }

    @PatchMapping("/api/admin/alert-types/{id}/deactivate")
    @RequiresPermission(RbacPermissions.ALERT_TYPES_UPDATE)
    public AlertTypeResponse deactivate(@PathVariable UUID id) {
        return AlertTypeMapper.toResponse(alertTypeService.deactivate(id));
    }

    @DeleteMapping("/api/admin/alert-types/{id}")
    @RequiresPermission(RbacPermissions.ALERT_TYPES_DELETE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        alertTypeService.delete(id);
    }
}
