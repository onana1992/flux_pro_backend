package com.nanotech.flux_pro_backend.auth;

import com.nanotech.flux_pro_backend.utilisateur.Utilisateur;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LoginAuditService {

    private final LoginAuditRepository loginAuditRepository;

    @Transactional
    public void log(Utilisateur utilisateur, String email, boolean success, HttpServletRequest request, String motif) {
        LoginAudit audit = new LoginAudit();
        audit.setUtilisateur(utilisateur);
        audit.setEmail(email);
        audit.setSucces(success);
        audit.setIpAddress(resolveIp(request));
        audit.setUserAgent(request.getHeader("User-Agent"));
        audit.setMotifEchec(motif);
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
