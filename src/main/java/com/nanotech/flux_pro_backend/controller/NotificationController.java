package com.nanotech.flux_pro_backend.controller;

import com.nanotech.flux_pro_backend.common.FileException;
import com.nanotech.flux_pro_backend.dto.response.AlertResponse;
import com.nanotech.flux_pro_backend.dto.response.UnreadCountResponse;
import com.nanotech.flux_pro_backend.entity.FileEntity;
import com.nanotech.flux_pro_backend.mapper.AlertMapper;
import com.nanotech.flux_pro_backend.repository.FileRepository;
import com.nanotech.flux_pro_backend.security.AccessControlService;
import com.nanotech.flux_pro_backend.security.RbacPermissions;
import com.nanotech.flux_pro_backend.security.RequiresPermission;
import com.nanotech.flux_pro_backend.security.SecurityUtils;
import com.nanotech.flux_pro_backend.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** Notifications in-app (ALR-05) de l'utilisateur connecté, sans permission dédiée : chacun voit les siennes. */
@RestController
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final SecurityUtils securityUtils;
    private final FileRepository fileRepository;
    private final AccessControlService accessControlService;

    @GetMapping("/api/notifications")
    public Page<AlertResponse> list(
            @RequestParam(defaultValue = "false") boolean unreadOnly,
            @PageableDefault(size = 20) Pageable pageable) {
        UUID userId = securityUtils.currentUser().getId();
        return notificationService.listForUser(userId, unreadOnly, pageable).map(AlertMapper::toResponse);
    }

    @GetMapping("/api/notifications/unread-count")
    public UnreadCountResponse unreadCount() {
        UUID userId = securityUtils.currentUser().getId();
        return new UnreadCountResponse(notificationService.countUnread(userId));
    }

    @PatchMapping("/api/notifications/{id}/read")
    public AlertResponse markRead(@PathVariable UUID id) {
        UUID userId = securityUtils.currentUser().getId();
        return AlertMapper.toResponse(notificationService.markRead(userId, id));
    }

    @PatchMapping("/api/notifications/read-all")
    public void markAllRead() {
        UUID userId = securityUtils.currentUser().getId();
        notificationService.markAllRead(userId);
    }

    /** Historique des alertes générées pour un dossier (audit, ALR-R09). */
    @GetMapping("/api/files/{fileId}/alerts")
    @RequiresPermission(RbacPermissions.FILES_READ)
    public List<AlertResponse> listForFile(@PathVariable UUID fileId) {
        FileEntity file = fileRepository.findByIdWithDetails(fileId)
                .orElseThrow(() -> FileException.notFound("FILE_NOT_FOUND", "File not found"));
        accessControlService.assertCanAccessFile(securityUtils.currentUser(), file);
        return notificationService.listForFile(fileId).stream().map(AlertMapper::toResponse).toList();
    }
}
