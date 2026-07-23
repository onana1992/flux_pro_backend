package com.nanotech.flux_pro_backend.service;

import com.nanotech.flux_pro_backend.common.AppException;
import com.nanotech.flux_pro_backend.entity.Alert;
import com.nanotech.flux_pro_backend.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;

/**
 * Envoi email (ALR-05). Le contenu est piloté par AlertType (label, emailTemplateCode) —
 * jamais par un type figé — de sorte qu'un nouveau type d'alerte créé par un admin produit
 * automatiquement un email cohérent sans changement de code (cf. docs/SPEC-ALR.md §9.2).
 */
@Service
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final TenantSettingsService tenantSettingsService;

    public EmailService(JavaMailSender mailSender, TenantSettingsService tenantSettingsService) {
        this.mailSender = mailSender;
        this.tenantSettingsService = tenantSettingsService;
    }

    public void send(Alert alert) {
        User recipient = alert.getRecipient();
        if (recipient.getEmail() == null || recipient.getEmail().isBlank()) {
            throw AppException.badRequest(
                    "ALERT_RECIPIENT_NO_EMAIL", "Recipient has no email address: " + recipient.getId(),
                    recipient.getId());
        }
        String intendedTo = recipient.getEmail();
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(tenantSettingsService.fromAddress());
        message.setTo(resolveTo(intendedTo));
        message.setSubject(buildSubject(alert));
        message.setText(withRedirectNotice(buildBody(alert), intendedTo));
        mailSender.send(message);
    }

    public void sendDigest(String toEmail, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(tenantSettingsService.fromAddress());
        message.setTo(resolveTo(toEmail));
        message.setSubject(subject);
        message.setText(withRedirectNotice(body, toEmail));
        mailSender.send(message);
    }

    private String resolveTo(String intendedTo) {
        String redirectTo = tenantSettingsService.emailRedirectTo();
        if (redirectTo.isEmpty()) {
            return intendedTo;
        }
        log.info("ALR: redirection email {} → {}", intendedTo, redirectTo);
        return redirectTo;
    }

    private String withRedirectNotice(String body, String intendedTo) {
        String redirectTo = tenantSettingsService.emailRedirectTo();
        if (redirectTo.isEmpty() || intendedTo == null || intendedTo.equalsIgnoreCase(redirectTo)) {
            return body;
        }
        return "[DEV — destinataire prévu : " + intendedTo + "]\n\n" + body;
    }

    private String buildSubject(Alert alert) {
        StringBuilder sb = new StringBuilder("[")
                .append(tenantSettingsService.productName())
                .append("] ")
                .append(alert.getAlertType().getLabel());
        if (alert.getFile() != null && alert.getFile().getReferenceNumber() != null) {
            sb.append(" — ").append(alert.getFile().getReferenceNumber());
        }
        return sb.toString();
    }

    private String buildBody(Alert alert) {
        DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
                .withZone(tenantSettingsService.zoneId());
        StringBuilder sb = new StringBuilder();
        sb.append(alert.getAlertType().getLabel()).append("\n\n");
        if (alert.getFile() != null) {
            sb.append("Dossier : ").append(alert.getFile().getReferenceNumber())
                    .append(" — ").append(alert.getFile().getSubject()).append("\n");
        }
        if (alert.getFilePassage() != null) {
            sb.append("Étape : ").append(alert.getFilePassage().getChainStepTemplate().getLabel()).append("\n");
            if (alert.getFilePassage().getDueAt() != null) {
                sb.append("Échéance : ").append(dateFormat.format(alert.getFilePassage().getDueAt())).append("\n");
            }
        }
        if (alert.getEscalationLevel() != null) {
            sb.append("Niveau d'escalade : ").append(alert.getEscalationLevel()).append("\n");
        }
        sb.append("\nConnectez-vous à ")
                .append(tenantSettingsService.productName())
                .append(" pour traiter ce dossier.");
        return sb.toString();
    }
}
