package com.nanotech.flux_pro_backend.service;

import com.nanotech.flux_pro_backend.entity.AdminAuditLog;
import com.nanotech.flux_pro_backend.repository.AdminAuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Persistance du journal d'audit des actions d'administration (création/modification/exécution).
 * Écrit dans une transaction indépendante (REQUIRES_NEW) afin que la trace soit conservée même si
 * l'action métier échoue, et pour ne jamais faire échouer l'action métier si l'écriture de l'audit
 * rencontre un problème.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdminAuditLogService {

    private final AdminAuditLogRepository adminAuditLogRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(UUID actorUserId, String actorEmail, String resourceType, String action,
                     String resourceId, String resourceLabel, boolean success, String errorMessage,
                     String ipAddress, String userAgent) {
        try {
            AdminAuditLog entry = new AdminAuditLog();
            entry.setActorUserId(actorUserId);
            entry.setActorEmail(actorEmail);
            entry.setResourceType(resourceType);
            entry.setAction(action);
            entry.setResourceId(resourceId);
            entry.setResourceLabel(resourceLabel);
            entry.setSuccess(success);
            entry.setErrorMessage(truncate(errorMessage, 500));
            entry.setIpAddress(ipAddress);
            entry.setUserAgent(userAgent);
            adminAuditLogRepository.save(entry);
        } catch (Exception e) {
            log.warn("Failed to persist admin audit log entry ({}:{}): {}", resourceType, action, e.getMessage());
        }
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
