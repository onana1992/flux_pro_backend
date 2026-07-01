package com.nanotech.flux_pro_backend.controller;

import com.nanotech.flux_pro_backend.dto.request.CreatePermissionRequest;
import com.nanotech.flux_pro_backend.dto.request.UpdatePermissionRequest;
import com.nanotech.flux_pro_backend.dto.response.PermissionResponse;
import com.nanotech.flux_pro_backend.mapper.DtoMapper;
import com.nanotech.flux_pro_backend.security.RbacPermissions;
import com.nanotech.flux_pro_backend.security.RequiresPermission;
import com.nanotech.flux_pro_backend.service.PermissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/permissions")
@RequiredArgsConstructor
public class PermissionController {

    private final PermissionService permissionService;

    @GetMapping
    @RequiresPermission(RbacPermissions.PERMISSIONS_READ)
    public Page<PermissionResponse> search(
            @RequestParam(required = false) String resource,
            @PageableDefault(size = 50) Pageable pageable) {
        return permissionService.search(resource, pageable).map(DtoMapper::toPermissionResponse);
    }

    @GetMapping("/{id}")
    @RequiresPermission(RbacPermissions.PERMISSIONS_READ)
    public PermissionResponse getById(@PathVariable UUID id) {
        return DtoMapper.toPermissionResponse(permissionService.getById(id));
    }

    @PostMapping
    @RequiresPermission(RbacPermissions.PERMISSIONS_CREATE)
    @ResponseStatus(HttpStatus.CREATED)
    public PermissionResponse create(@Valid @RequestBody CreatePermissionRequest request) {
        return DtoMapper.toPermissionResponse(permissionService.create(request));
    }

    @PutMapping("/{id}")
    @RequiresPermission(RbacPermissions.PERMISSIONS_UPDATE)
    public PermissionResponse update(@PathVariable UUID id, @Valid @RequestBody UpdatePermissionRequest request) {
        return DtoMapper.toPermissionResponse(permissionService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @RequiresPermission(RbacPermissions.PERMISSIONS_DELETE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        permissionService.delete(id);
    }
}
