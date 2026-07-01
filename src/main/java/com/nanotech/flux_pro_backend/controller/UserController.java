package com.nanotech.flux_pro_backend.controller;

import com.nanotech.flux_pro_backend.common.ImportResult;
import com.nanotech.flux_pro_backend.dto.request.AssignRoleRequest;
import com.nanotech.flux_pro_backend.dto.request.UserRequest;
import com.nanotech.flux_pro_backend.dto.response.ResetPasswordResponse;
import com.nanotech.flux_pro_backend.dto.response.UserProfileResponse;
import com.nanotech.flux_pro_backend.dto.response.UserResponse;
import com.nanotech.flux_pro_backend.enumeration.UserRole;
import com.nanotech.flux_pro_backend.security.RbacPermissions;
import com.nanotech.flux_pro_backend.security.RequiresPermission;
import com.nanotech.flux_pro_backend.security.SecurityUtils;
import com.nanotech.flux_pro_backend.service.RoleService;
import com.nanotech.flux_pro_backend.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final RoleService roleService;
    private final SecurityUtils securityUtils;

    @GetMapping("/me")
    public UserProfileResponse me() {
        return userService.getMeProfile(securityUtils.currentUser());
    }

    @GetMapping
    @RequiresPermission(RbacPermissions.USERS_READ)
    public Page<UserResponse> search(
            @RequestParam(required = false) UUID organizationId,
            @RequestParam(required = false) UserRole role,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20) Pageable pageable) {
        return userService.search(securityUtils.currentUser(), organizationId, role, search, pageable);
    }

    @GetMapping("/{id}")
    @RequiresPermission(RbacPermissions.USERS_READ)
    public UserResponse getById(@PathVariable UUID id) {
        return userService.getById(securityUtils.currentUser(), id);
    }

    @PostMapping
    @RequiresPermission(RbacPermissions.USERS_CREATE)
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> create(@Valid @RequestBody UserRequest request) {
        var result = userService.create(securityUtils.currentUser(), request);
        return Map.of(
                "user", result.user(),
                "temporaryPassword", result.temporaryPassword());
    }

    @PutMapping("/{id}")
    @RequiresPermission(RbacPermissions.USERS_UPDATE)
    public UserResponse update(@PathVariable UUID id, @Valid @RequestBody UserRequest request) {
        return userService.update(securityUtils.currentUser(), id, request);
    }

    @PatchMapping("/{id}/deactivate")
    @RequiresPermission(RbacPermissions.USERS_UPDATE)
    public UserResponse deactivate(@PathVariable UUID id) {
        return userService.deactivate(securityUtils.currentUser(), id);
    }

    @PatchMapping("/{id}/activate")
    @RequiresPermission(RbacPermissions.USERS_UPDATE)
    public UserResponse activate(@PathVariable UUID id) {
        return userService.activate(securityUtils.currentUser(), id);
    }

    @PatchMapping("/{id}/unlock")
    @RequiresPermission(RbacPermissions.USERS_UNLOCK)
    public UserResponse unlock(@PathVariable UUID id) {
        return userService.unlock(securityUtils.currentUser(), id);
    }

    @PostMapping("/{id}/reset-password")
    @RequiresPermission(RbacPermissions.USERS_RESET_PASSWORD)
    public ResetPasswordResponse resetPassword(@PathVariable UUID id) {
        return userService.resetPassword(securityUtils.currentUser(), id);
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @RequiresPermission(RbacPermissions.USERS_IMPORT)
    public ImportResult importCsv(@RequestParam("file") MultipartFile file) throws IOException {
        return userService.importCsv(securityUtils.currentUser(), file);
    }

    @PostMapping("/{id}/roles")
    @RequiresPermission(RbacPermissions.USERS_UPDATE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void assignRole(@PathVariable UUID id, @Valid @RequestBody AssignRoleRequest request) {
        userService.assertCanManageUser(securityUtils.currentUser(), id);
        roleService.assignRoleToUser(id, request.roleId());
    }

    @DeleteMapping("/{id}/roles/{roleId}")
    @RequiresPermission(RbacPermissions.USERS_UPDATE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revokeRole(@PathVariable UUID id, @PathVariable UUID roleId) {
        userService.assertCanManageUser(securityUtils.currentUser(), id);
        roleService.revokeRoleFromUser(id, roleId);
    }
}
