package com.nanotech.flux_pro_backend.controller;

import com.nanotech.flux_pro_backend.dto.request.ChainInitializeRequest;
import com.nanotech.flux_pro_backend.dto.response.FilePassageCircuitResponse;
import com.nanotech.flux_pro_backend.dto.response.PassageCandidateResponse;
import com.nanotech.flux_pro_backend.enumeration.UserRole;
import com.nanotech.flux_pro_backend.security.RbacPermissions;
import com.nanotech.flux_pro_backend.security.RequiresPermission;
import com.nanotech.flux_pro_backend.security.SecurityUtils;
import com.nanotech.flux_pro_backend.service.PassageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/files/{fileId}/chain")
@RequiredArgsConstructor
public class FileChainController {

    private final PassageService passageService;
    private final SecurityUtils securityUtils;

    @PostMapping("/initialize")
    @RequiresPermission(RbacPermissions.FILES_UPDATE)
    public FilePassageCircuitResponse initialize(
            @PathVariable UUID fileId,
            @Valid @RequestBody ChainInitializeRequest request) {
        return passageService.initializeChainForFile(fileId, request, securityUtils.currentUser());
    }

    @GetMapping("/candidates")
    @RequiresPermission(RbacPermissions.FILES_UPDATE)
    public List<PassageCandidateResponse> candidates(
            @PathVariable UUID fileId,
            @RequestParam UserRole role) {
        return passageService.listCandidates(fileId, role, securityUtils.currentUser());
    }
}
