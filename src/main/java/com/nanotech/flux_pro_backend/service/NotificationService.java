package com.nanotech.flux_pro_backend.service;

import com.nanotech.flux_pro_backend.common.AlertException;
import com.nanotech.flux_pro_backend.entity.Alert;
import com.nanotech.flux_pro_backend.enumeration.AlertChannel;
import com.nanotech.flux_pro_backend.enumeration.AlertStatus;
import com.nanotech.flux_pro_backend.repository.AlertRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Distribution multicanal (ALR-05) et accès aux notifications in-app de l'utilisateur courant.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final AlertRepository alertRepository;
    private final EmailService emailService;
    private final ClockService clockService;

    public List<AlertChannel> activeChannels() {
        return List.of(AlertChannel.IN_APP, AlertChannel.EMAIL);
    }

    /**
     * Transaction dédiée : un échec SMTP ne doit pas marquer rollback-only
     * la transaction du moteur d'alertes (sinon l'IN_APP disparaît aussi).
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void dispatch(Alert alert) {
        switch (alert.getChannel()) {
            case IN_APP -> markSent(alert);
            case EMAIL -> sendVia(alert, emailService::send);
        }
    }

    private void markSent(Alert alert) {
        alert.setStatus(AlertStatus.SENT);
        alert.setSentAt(clockService.now());
        alertRepository.save(alert);
    }

    private void sendVia(Alert alert, java.util.function.Consumer<Alert> sender) {
        try {
            sender.accept(alert);
            alert.setStatus(AlertStatus.SENT);
            alert.setSentAt(clockService.now());
        } catch (Exception e) {
            log.warn("ALR: envoi {} échoué pour l'alerte {} : {}", alert.getChannel(), alert.getId(), e.getMessage());
            alert.setStatus(AlertStatus.FAILED);
            alert.setErrorMessage(truncate(e.getMessage()));
        }
        alertRepository.save(alert);
    }

    private String truncate(String message) {
        if (message == null || message.isBlank()) {
            return "Erreur inconnue";
        }
        return message.length() > 500 ? message.substring(0, 500) : message;
    }

    @Transactional(readOnly = true)
    public Page<Alert> listForUser(UUID userId, boolean unreadOnly, Pageable pageable) {
        return unreadOnly
                ? alertRepository.findByRecipientIdAndChannelAndUnread(userId, AlertChannel.IN_APP, pageable)
                : alertRepository.findByRecipientIdAndChannel(userId, AlertChannel.IN_APP, pageable);
    }

    @Transactional(readOnly = true)
    public long countUnread(UUID userId) {
        return alertRepository.countByRecipientIdAndChannelAndReadAtIsNull(userId, AlertChannel.IN_APP);
    }

    @Transactional
    public Alert markRead(UUID userId, UUID alertId) {
        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> AlertException.notFound("ALERT_NOT_FOUND", "Alert not found"));
        if (!alert.getRecipient().getId().equals(userId)) {
            throw AlertException.forbidden("ALERT_ACCESS_DENIED", "This notification does not belong to you");
        }
        alert.setReadAt(clockService.now());
        if (alert.getStatus() == AlertStatus.SENT) {
            alert.setStatus(AlertStatus.READ);
        }
        return alertRepository.save(alert);
    }

    @Transactional
    public void markAllRead(UUID userId) {
        List<Alert> unread = alertRepository.findByRecipientIdAndChannelAndReadAtIsNull(userId, AlertChannel.IN_APP);
        Instant now = clockService.now();
        for (Alert alert : unread) {
            alert.setReadAt(now);
            alert.setStatus(AlertStatus.READ);
        }
        alertRepository.saveAll(unread);
    }

    @Transactional(readOnly = true)
    public List<Alert> listForFile(UUID fileId) {
        return alertRepository.findByFileIdOrderByCreatedAtDesc(fileId);
    }
}
