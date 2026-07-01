package com.nanotech.flux_pro_backend.auth;

import com.nanotech.flux_pro_backend.auth.dto.LoginAuditResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api/admin/login-audit")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class LoginAuditController {

    private final LoginAuditRepository loginAuditRepository;

    @GetMapping
    public Page<LoginAuditResponse> search(
            @RequestParam(required = false) String email,
            @RequestParam(required = false) Boolean succes,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @PageableDefault(size = 50) Pageable pageable) {
        return loginAuditRepository.search(email, succes, from, to, pageable)
                .map(a -> new LoginAuditResponse(
                        a.getId(),
                        a.getEmail(),
                        a.isSucces(),
                        a.getIpAddress(),
                        a.getUserAgent(),
                        a.getMotifEchec(),
                        a.getCreatedAt()));
    }
}
