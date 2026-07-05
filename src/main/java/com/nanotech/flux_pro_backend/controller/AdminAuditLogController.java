package com.nanotech.flux_pro_backend.controller;

import com.nanotech.flux_pro_backend.dto.response.AdminAuditLogResponse;
import com.nanotech.flux_pro_backend.repository.AdminAuditLogRepository;
import com.nanotech.flux_pro_backend.security.RbacPermissions;
import com.nanotech.flux_pro_backend.security.RequiresPermission;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api/admin/audit-log")
@RequiredArgsConstructor
@RequiresPermission(RbacPermissions.AUDIT_LOG_READ)
public class AdminAuditLogController {

    private final AdminAuditLogRepository adminAuditLogRepository;

    @GetMapping
    public Page<AdminAuditLogResponse> search(
            @RequestParam(required = false) String resourceType,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String actorEmail,
            @RequestParam(required = false) Boolean success,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @PageableDefault(size = 50) Pageable pageable) {
        return adminAuditLogRepository.search(resourceType, action, actorEmail, success, from, to, pageable)
                .map(a -> new AdminAuditLogResponse(
                        a.getId(),
                        a.getActorEmail(),
                        a.getResourceType(),
                        a.getAction(),
                        a.getResourceId(),
                        a.getResourceLabel(),
                        a.isSuccess(),
                        a.getErrorMessage(),
                        a.getIpAddress(),
                        a.getUserAgent(),
                        a.getCreatedAt()));
    }
}
