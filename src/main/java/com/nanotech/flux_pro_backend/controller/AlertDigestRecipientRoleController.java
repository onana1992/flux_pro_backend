package com.nanotech.flux_pro_backend.controller;

import com.nanotech.flux_pro_backend.dto.request.AlertDigestRecipientRolesRequest;
import com.nanotech.flux_pro_backend.dto.response.AlertDigestRecipientRolesResponse;
import com.nanotech.flux_pro_backend.enumeration.UserRole;
import com.nanotech.flux_pro_backend.security.RbacPermissions;
import com.nanotech.flux_pro_backend.security.RequiresPermission;
import com.nanotech.flux_pro_backend.service.AlertDigestRecipientRoleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/alert-digest/recipient-roles")
@RequiredArgsConstructor
public class AlertDigestRecipientRoleController {

    private final AlertDigestRecipientRoleService service;

    @GetMapping
    @RequiresPermission(RbacPermissions.ALERT_RULES_READ)
    public AlertDigestRecipientRolesResponse list() {
        return new AlertDigestRecipientRolesResponse(service.listRoles());
    }

    @PutMapping
    @RequiresPermission(RbacPermissions.ALERT_RULES_UPDATE)
    public AlertDigestRecipientRolesResponse replace(@Valid @RequestBody AlertDigestRecipientRolesRequest request) {
        return new AlertDigestRecipientRolesResponse(service.replaceRoles(request.roles()));
    }

    @PostMapping("/{role}")
    @RequiresPermission(RbacPermissions.ALERT_RULES_UPDATE)
    public AlertDigestRecipientRolesResponse add(@PathVariable UserRole role) {
        return new AlertDigestRecipientRolesResponse(service.addRole(role));
    }

    @DeleteMapping("/{role}")
    @RequiresPermission(RbacPermissions.ALERT_RULES_UPDATE)
    public AlertDigestRecipientRolesResponse remove(@PathVariable UserRole role) {
        return new AlertDigestRecipientRolesResponse(service.removeRole(role));
    }
}
