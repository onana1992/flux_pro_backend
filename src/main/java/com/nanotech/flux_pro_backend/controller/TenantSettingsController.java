package com.nanotech.flux_pro_backend.controller;

import com.nanotech.flux_pro_backend.dto.request.TenantSettingsRequest;
import com.nanotech.flux_pro_backend.dto.response.TenantConfigResponse;
import com.nanotech.flux_pro_backend.security.RbacPermissions;
import com.nanotech.flux_pro_backend.security.RequiresPermission;
import com.nanotech.flux_pro_backend.service.TenantSettingsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class TenantSettingsController {

    private final TenantSettingsService tenantSettingsService;

    /** Config publique (login / branding) — sans authentification. */
    @GetMapping("/api/public/tenant-config")
    public TenantConfigResponse publicConfig() {
        return tenantSettingsService.toResponse();
    }

    @GetMapping("/api/admin/tenant-settings")
    @RequiresPermission(RbacPermissions.BUSINESS_CALENDAR_READ)
    public TenantConfigResponse get() {
        return tenantSettingsService.toResponse();
    }

    @PutMapping("/api/admin/tenant-settings")
    @RequiresPermission(RbacPermissions.BUSINESS_CALENDAR_UPDATE)
    public TenantConfigResponse update(@Valid @RequestBody TenantSettingsRequest request) {
        return tenantSettingsService.update(request);
    }
}
