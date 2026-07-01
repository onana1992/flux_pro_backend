package com.nanotech.flux_pro_backend.service;

import com.nanotech.flux_pro_backend.entity.LoginAudit;
import com.nanotech.flux_pro_backend.entity.User;
import com.nanotech.flux_pro_backend.repository.LoginAuditRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LoginAuditService {

    private final LoginAuditRepository loginAuditRepository;

    @Transactional
    public void log(User user, String email, boolean success, HttpServletRequest request, String failureReason) {
        LoginAudit audit = new LoginAudit();
        audit.setUser(user);
        audit.setEmail(email);
        audit.setSuccess(success);
        audit.setIpAddress(resolveIp(request));
        audit.setUserAgent(request.getHeader("User-Agent"));
        audit.setFailureReason(failureReason);
        loginAuditRepository.save(audit);
    }

    private String resolveIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
