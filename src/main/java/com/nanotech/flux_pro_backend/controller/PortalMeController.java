package com.nanotech.flux_pro_backend.controller;

import com.nanotech.flux_pro_backend.common.AppException;
import com.nanotech.flux_pro_backend.dto.response.PortalUserProfileResponse;
import com.nanotech.flux_pro_backend.repository.PortalUserRepository;
import com.nanotech.flux_pro_backend.security.PortalSecurityUser;
import com.nanotech.flux_pro_backend.security.SecurityUtils;
import com.nanotech.flux_pro_backend.service.portal.PortalAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Profil portail authentifié (realm {@code portal_access}).
 */
@RestController
@RequestMapping("/api/portal")
@RequiredArgsConstructor
public class PortalMeController {

    private final SecurityUtils securityUtils;
    private final PortalUserRepository portalUserRepository;

    @GetMapping("/me")
    public ResponseEntity<PortalUserProfileResponse> me() {
        PortalSecurityUser actor = securityUtils.currentPortalUser();
        var user = portalUserRepository.findById(actor.getId())
                .orElseThrow(() -> AppException.notFound("PORTAL_USER_NOT_FOUND", "Portal user not found"));
        return ResponseEntity.ok(PortalAuthService.toProfile(user));
    }
}
