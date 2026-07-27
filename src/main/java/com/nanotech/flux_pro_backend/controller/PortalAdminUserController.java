package com.nanotech.flux_pro_backend.controller;

import com.nanotech.flux_pro_backend.dto.request.PortalUserCreateRequest;
import com.nanotech.flux_pro_backend.dto.request.PortalUserResetPasswordRequest;
import com.nanotech.flux_pro_backend.dto.request.PortalUserUpdateRequest;
import com.nanotech.flux_pro_backend.dto.response.PortalUserAdminResponse;
import com.nanotech.flux_pro_backend.security.RbacPermissions;
import com.nanotech.flux_pro_backend.security.RequiresPermission;
import com.nanotech.flux_pro_backend.security.SecurityUtils;
import com.nanotech.flux_pro_backend.service.portal.PortalAdminUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/portal-users")
@RequiredArgsConstructor
public class PortalAdminUserController {

    private final PortalAdminUserService portalAdminUserService;
    private final SecurityUtils securityUtils;

    @GetMapping
    @RequiresPermission(RbacPermissions.USERS_READ)
    public List<PortalUserAdminResponse> list() {
        return portalAdminUserService.listAll();
    }

    @PostMapping
    @RequiresPermission(RbacPermissions.USERS_CREATE)
    @ResponseStatus(HttpStatus.CREATED)
    public PortalUserAdminResponse create(@Valid @RequestBody PortalUserCreateRequest request) {
        return portalAdminUserService.createInternal(request, securityUtils.currentUser());
    }

    @PatchMapping("/{id}")
    @RequiresPermission(RbacPermissions.USERS_UPDATE)
    public PortalUserAdminResponse update(
            @PathVariable UUID id,
            @Valid @RequestBody PortalUserUpdateRequest request) {
        return portalAdminUserService.update(id, request);
    }

    @PostMapping("/{id}/reset-password")
    @RequiresPermission(RbacPermissions.USERS_RESET_PASSWORD)
    public PortalUserAdminResponse resetPassword(
            @PathVariable UUID id,
            @RequestBody(required = false) PortalUserResetPasswordRequest request) {
        return portalAdminUserService.resetPassword(id, request, securityUtils.currentUser());
    }

    @PostMapping("/{id}/active")
    @RequiresPermission(RbacPermissions.USERS_UPDATE)
    public PortalUserAdminResponse setActive(
            @PathVariable UUID id, @RequestParam boolean active) {
        return portalAdminUserService.setActive(id, active);
    }
}
