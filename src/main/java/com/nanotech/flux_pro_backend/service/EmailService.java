package com.nanotech.flux_pro_backend.service;

import com.nanotech.flux_pro_backend.entity.Alert;
import com.nanotech.flux_pro_backend.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Envoi email (ALR-05). Le contenu est piloté par AlertType (label, emailTemplateCode) —
 * jamais par un type figé — de sorte qu'un nouveau type d'alerte créé par un admin produit
 * automatiquement un email cohérent sans changement de code (cf. docs/SPEC-ALR.md §9.2).
 */
@Service
@Slf4j
public class EmailService {

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.of("Africa/Douala"));

    private final JavaMailSender mailSender;
    private final String fromAddress;

    public EmailService(
            JavaMailSender mailSender,
            @Value("${fluxpro.alerts.from-address:alertes@mintp.cm}") String fromAddress) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
    }

    public void send(Alert alert) {
        User recipient = alert.getRecipient();
        if (recipient.getEmail() == null || recipient.getEmail().isBlank()) {
            throw new IllegalStateException("Recipient has no email address: " + recipient.getId());
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(recipient.getEmail());
        message.setSubject(buildSubject(alert));
        message.setText(buildBody(alert));
        mailSender.send(message);
    }

    public void sendDigest(String toEmail, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(toEmail);
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
    }

    private String buildSubject(Alert alert) {
        StringBuilder sb = new StringBuilder("[ChaîneFlux] ").append(alert.getAlertType().getLabel());
        if (alert.getFile() != null && alert.getFile().getReferenceNumber() != null) {
            sb.append(" — ").append(alert.getFile().getReferenceNumber());
        }
        return sb.toString();
    }

    private String buildBody(Alert alert) {
        StringBuilder sb = new StringBuilder();
        sb.append(alert.getAlertType().getLabel()).append("\n\n");
        if (alert.getFile() != null) {
            sb.append("Dossier : ").append(alert.getFile().getReferenceNumber())
                    .append(" — ").append(alert.getFile().getSubject()).append("\n");
        }
        if (alert.getFilePassage() != null) {
            sb.append("Étape : ").append(alert.getFilePassage().getChainStepTemplate().getLabel()).append("\n");
            if (alert.getFilePassage().getDueAt() != null) {
                sb.append("Échéance : ").append(DATE_FORMAT.format(alert.getFilePassage().getDueAt())).append("\n");
            }
        }
        if (alert.getEscalationLevel() != null) {
            sb.append("Niveau d'escalade : ").append(alert.getEscalationLevel()).append("\n");
        }
        sb.append("\nConnectez-vous à ChaîneFlux pour traiter ce dossier.");
        return sb.toString();
    }
}
