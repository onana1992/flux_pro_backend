package com.nanotech.flux_pro_backend.controller;

import com.nanotech.flux_pro_backend.dto.response.LoginAuditResponse;
import com.nanotech.flux_pro_backend.repository.LoginAuditRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import com.nanotech.flux_pro_backend.security.RbacPermissions;
import com.nanotech.flux_pro_backend.security.RequiresPermission;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api/admin/login-audit")
@RequiredArgsConstructor
@RequiresPermission(RbacPermissions.LOGIN_AUDIT_READ)
public class LoginAuditController {

    private final LoginAuditRepository loginAuditRepository;

    @GetMapping
    public Page<LoginAuditResponse> search(
            @RequestParam(required = false) String email,
            @RequestParam(required = false) Boolean success,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @PageableDefault(size = 50) Pageable pageable) {
        return loginAuditRepository.search(email, success, from, to, pageable)
                .map(a -> new LoginAuditResponse(
                        a.getId(),
                        a.getEmail(),
                        a.isSuccess(),
                        a.getIpAddress(),
                        a.getUserAgent(),
                        a.getFailureReason(),
                        a.getCreatedAt()));
    }
}
