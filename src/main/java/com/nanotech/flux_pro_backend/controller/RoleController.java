package com.nanotech.flux_pro_backend.controller;

import com.nanotech.flux_pro_backend.dto.request.AssignPermissionsRequest;
import com.nanotech.flux_pro_backend.dto.request.CreateRoleRequest;
import com.nanotech.flux_pro_backend.dto.request.UpdateRoleRequest;
import com.nanotech.flux_pro_backend.dto.response.RoleResponse;
import com.nanotech.flux_pro_backend.mapper.DtoMapper;
import com.nanotech.flux_pro_backend.security.RbacPermissions;
import com.nanotech.flux_pro_backend.security.RequiresPermission;
import com.nanotech.flux_pro_backend.service.RoleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @GetMapping
    @RequiresPermission(RbacPermissions.ROLES_READ)
    public List<RoleResponse> list() {
        return roleService.listAll().stream().map(DtoMapper::toRoleResponse).toList();
    }

    @GetMapping("/{id}")
    @RequiresPermission(RbacPermissions.ROLES_READ)
    public RoleResponse getById(@PathVariable UUID id) {
        return DtoMapper.toRoleResponse(roleService.getById(id));
    }

    @PostMapping
    @RequiresPermission(RbacPermissions.ROLES_CREATE)
    @ResponseStatus(HttpStatus.CREATED)
    public RoleResponse create(@Valid @RequestBody CreateRoleRequest request) {
        return DtoMapper.toRoleResponse(roleService.create(request));
    }

    @PutMapping("/{id}")
    @RequiresPermission(RbacPermissions.ROLES_UPDATE)
    public RoleResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateRoleRequest request) {
        return DtoMapper.toRoleResponse(roleService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @RequiresPermission(RbacPermissions.ROLES_DELETE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        roleService.delete(id);
    }

    @PostMapping("/{id}/permissions")
    @RequiresPermission(RbacPermissions.ROLES_UPDATE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void assignPermissions(@PathVariable UUID id, @Valid @RequestBody AssignPermissionsRequest request) {
        roleService.assignPermissions(id, request);
    }

    @DeleteMapping("/{id}/permissions/{permissionId}")
    @RequiresPermission(RbacPermissions.ROLES_UPDATE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revokePermission(@PathVariable UUID id, @PathVariable UUID permissionId) {
        roleService.revokePermission(id, permissionId);
    }
}
