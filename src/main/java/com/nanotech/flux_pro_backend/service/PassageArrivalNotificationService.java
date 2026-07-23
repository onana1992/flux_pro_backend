package com.nanotech.flux_pro_backend.service;

import com.nanotech.flux_pro_backend.entity.Alert;
import com.nanotech.flux_pro_backend.entity.AlertType;
import com.nanotech.flux_pro_backend.entity.FileEntity;
import com.nanotech.flux_pro_backend.entity.FilePassage;
import com.nanotech.flux_pro_backend.entity.User;
import com.nanotech.flux_pro_backend.enumeration.AlertChannel;
import com.nanotech.flux_pro_backend.enumeration.AlertStatus;
import com.nanotech.flux_pro_backend.repository.AlertRepository;
import com.nanotech.flux_pro_backend.repository.AlertTypeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Notifications d'arrivée d'un dossier sur un maillon : responsable + copies informées (CHN-09).
 * Canaux : IN_APP + EMAIL (même stack que ALR-05).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PassageArrivalNotificationService {

    public static final String TYPE_ARRIVAL = "PASSAGE_ARRIVAL";
    public static final String TYPE_CC = "PASSAGE_CC";

    private final AlertTypeRepository alertTypeRepository;
    private final AlertRepository alertRepository;
    private final NotificationService notificationService;
    private final SubstituteService substituteService;

    @Transactional
    public void notifyArrival(FilePassage passage) {
        if (passage == null || passage.getId() == null) {
            return;
        }
        FileEntity file = passage.getFile();
        User responsible = passage.getResponsibleUser();
        Instant since = passage.getReceivedAt() != null ? passage.getReceivedAt() : Instant.EPOCH;

        Set<UUID> notified = new LinkedHashSet<>();

        if (responsible != null && responsible.isActive()) {
            User recipient = substituteService.effectiveRecipient(responsible);
            if (recipient != null && recipient.isActive()) {
                notifyUser(passage, file, recipient, TYPE_ARRIVAL, since);
                notified.add(recipient.getId());
            }
        }

        List<User> ccUsers = passage.getCcUsers() != null ? passage.getCcUsers() : List.of();
        for (User cc : ccUsers) {
            if (cc == null || !cc.isActive() || notified.contains(cc.getId())) {
                continue;
            }
            notifyUser(passage, file, cc, TYPE_CC, since);
            notified.add(cc.getId());
        }
    }

    private void notifyUser(
            FilePassage passage, FileEntity file, User recipient, String typeCode, Instant since) {
        AlertType type = alertTypeRepository.findByCodeIgnoreCase(typeCode).orElse(null);
        if (type == null || !type.isActive()) {
            log.warn("CHN: type d'alerte {} introuvable — notification d'arrivée ignorée", typeCode);
            return;
        }
        for (AlertChannel channel : notificationService.activeChannels()) {
            if (alertRepository.existsArrivalNotification(
                    passage.getId(), type.getId(), recipient.getId(), channel, since)) {
                continue;
            }
            Alert alert = new Alert();
            alert.setFile(file);
            alert.setFilePassage(passage);
            alert.setAlertType(type);
            alert.setChannel(channel);
            alert.setRecipient(recipient);
            alert.setStatus(AlertStatus.PENDING);
            alert = alertRepository.save(alert);
            try {
                notificationService.dispatch(alert);
            } catch (Exception e) {
                log.warn(
                        "CHN: dispatch {} échoué pour {} / passage {} : {}",
                        channel,
                        recipient.getId(),
                        passage.getId(),
                        e.getMessage());
            }
        }
    }
}
