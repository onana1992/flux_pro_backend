package com.nanotech.flux_pro_backend.controller;

import com.nanotech.flux_pro_backend.dto.request.PassageCommentRequest;
import com.nanotech.flux_pro_backend.dto.request.PassageReasonRequest;
import com.nanotech.flux_pro_backend.dto.request.PassageReassignRequest;
import com.nanotech.flux_pro_backend.dto.request.PassageReturnRequest;
import com.nanotech.flux_pro_backend.dto.request.PassageTransmitRequest;
import com.nanotech.flux_pro_backend.dto.response.CurrentHolderResponse;
import com.nanotech.flux_pro_backend.dto.response.FilePassageCircuitResponse;
import com.nanotech.flux_pro_backend.security.RbacPermissions;
import com.nanotech.flux_pro_backend.security.RequiresPermission;
import com.nanotech.flux_pro_backend.security.SecurityUtils;
import com.nanotech.flux_pro_backend.service.PassageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/files/{fileId}/passages")
@RequiredArgsConstructor
public class PassageController {

    private final PassageService passageService;
    private final SecurityUtils securityUtils;

    @GetMapping
    @RequiresPermission(RbacPermissions.FILES_READ)
    public FilePassageCircuitResponse list(@PathVariable UUID fileId) {
        return passageService.getCircuit(fileId, securityUtils.currentUser());
    }

    @GetMapping("/current")
    @RequiresPermission(RbacPermissions.FILES_READ)
    public CurrentHolderResponse current(@PathVariable UUID fileId) {
        return passageService.getCurrentHolder(fileId, securityUtils.currentUser());
    }

    @PostMapping("/{passageId}/transmit")
    @RequiresPermission(RbacPermissions.FILES_TRANSMIT)
    public FilePassageCircuitResponse transmit(
            @PathVariable UUID fileId,
            @PathVariable UUID passageId,
            @Valid @RequestBody(required = false) PassageTransmitRequest request) {
        PassageTransmitRequest body = request != null ? request : new PassageTransmitRequest(null, null, null);
        return passageService.transmit(fileId, passageId, body, securityUtils.currentUser());
    }

    @PostMapping("/{passageId}/return")
    @RequiresPermission(RbacPermissions.FILES_TRANSMIT)
    public FilePassageCircuitResponse returnPassage(
            @PathVariable UUID fileId,
            @PathVariable UUID passageId,
            @Valid @RequestBody PassageReturnRequest request) {
        return passageService.returnToPrevious(fileId, passageId, request, securityUtils.currentUser());
    }

    @PostMapping("/{passageId}/suspend")
    @RequiresPermission(RbacPermissions.FILES_TRANSMIT)
    public FilePassageCircuitResponse suspend(
            @PathVariable UUID fileId,
            @PathVariable UUID passageId,
            @Valid @RequestBody PassageReasonRequest request) {
        return passageService.suspend(fileId, passageId, request, securityUtils.currentUser());
    }

    @PostMapping("/{passageId}/resume")
    @RequiresPermission(RbacPermissions.FILES_TRANSMIT)
    public FilePassageCircuitResponse resume(@PathVariable UUID fileId, @PathVariable UUID passageId) {
        return passageService.resume(fileId, passageId, securityUtils.currentUser());
    }

    @PostMapping("/{passageId}/reassign")
    @RequiresPermission(RbacPermissions.FILES_UPDATE)
    public FilePassageCircuitResponse reassign(
            @PathVariable UUID fileId,
            @PathVariable UUID passageId,
            @Valid @RequestBody PassageReassignRequest request) {
        return passageService.reassign(fileId, passageId, request, securityUtils.currentUser());
    }

    @PatchMapping("/{passageId}/comment")
    @RequiresPermission(RbacPermissions.FILES_TRANSMIT)
    public FilePassageCircuitResponse updateComment(
            @PathVariable UUID fileId,
            @PathVariable UUID passageId,
            @Valid @RequestBody PassageCommentRequest request) {
        return passageService.updateInternalComment(fileId, passageId, request, securityUtils.currentUser());
    }
}
