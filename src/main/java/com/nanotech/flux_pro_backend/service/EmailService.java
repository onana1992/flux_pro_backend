package com.nanotech.flux_pro_backend.service;

import com.nanotech.flux_pro_backend.common.AppException;
import com.nanotech.flux_pro_backend.email.EmailDigestItem;
import com.nanotech.flux_pro_backend.email.EmailMessageModel;
import com.nanotech.flux_pro_backend.entity.Alert;
import com.nanotech.flux_pro_backend.entity.AlertType;
import com.nanotech.flux_pro_backend.entity.FileEntity;
import com.nanotech.flux_pro_backend.entity.FilePassage;
import com.nanotech.flux_pro_backend.entity.User;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Envoi email HTML (ALR-05 / ALR-08). Gabarit choisi via {@link AlertType#getEmailTemplateCode()}.
 */
@Service
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final TenantSettingsService tenantSettingsService;
    private final EmailTemplateService emailTemplateService;
    private final DelaiService delaiService;
    private final ClockService clockService;

    @Value("${fluxpro.alerts.app-base-url:http://localhost:3000}")
    private String appBaseUrl;

    public EmailService(
            JavaMailSender mailSender,
            TenantSettingsService tenantSettingsService,
            EmailTemplateService emailTemplateService,
            DelaiService delaiService,
            ClockService clockService) {
        this.mailSender = mailSender;
        this.tenantSettingsService = tenantSettingsService;
        this.emailTemplateService = emailTemplateService;
        this.delaiService = delaiService;
        this.clockService = clockService;
    }

    public void send(Alert alert) {
        User recipient = alert.getRecipient();
        if (recipient.getEmail() == null || recipient.getEmail().isBlank()) {
            throw AppException.badRequest(
                    "ALERT_RECIPIENT_NO_EMAIL", "Recipient has no email address: " + recipient.getId(),
                    recipient.getId());
        }
        String intendedTo = recipient.getEmail();
        AlertType type = alert.getAlertType();
        String templateCode = type != null ? type.getEmailTemplateCode() : null;
        EmailMessageModel model = buildAlertModel(alert, intendedTo);
        String html = emailTemplateService.render(templateCode, model);
        sendHtml(intendedTo, buildSubject(alert), html);
    }

    public void sendDigest(String toEmail, String recipientFirstName, List<EmailDigestItem> items) {
        if (toEmail == null || toEmail.isBlank() || items == null || items.isEmpty()) {
            return;
        }
        EmailMessageModel model = EmailMessageModel.builder()
                .productName(tenantSettingsService.productName())
                .tenantBadge(tenantSettingsService.current().getBadge())
                .alertLabel("Récapitulatif quotidien des retards")
                .alertDescription("Dossiers en retard dans votre périmètre organisationnel.")
                .intro("Voici les dossiers actuellement en retard dans votre périmètre ("
                        + items.size() + "). Merci de prioriser le suivi.")
                .tone("navy")
                .recipientFirstName(recipientFirstName)
                .ctaLabel("Accéder à FluxPro")
                .fileUrl(trimSlash(appBaseUrl))
                .digestItems(items)
                .redirectNotice(redirectNotice(toEmail))
                .build();
        String subject = "[" + tenantSettingsService.productName()
                + "] Récapitulatif quotidien des retards (" + items.size() + ")";
        String html = emailTemplateService.render("alert-daily-digest", model);
        sendHtml(toEmail, subject, html);
    }

    public EmailDigestItem toDigestItem(FilePassage passage, Instant now) {
        FileEntity file = passage.getFile();
        User responsible = passage.getResponsibleUser();
        String responsibleName = responsible == null
                ? "—"
                : displayName(responsible);
        DateTimeFormatter dueFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy")
                .withZone(tenantSettingsService.zoneId());
        int late = delaiService.countWorkingDays(passage.getDueAt(), now);
        return new EmailDigestItem(
                file.getReferenceNumber(),
                file.getSubject(),
                passage.getChainStepTemplate().getLabel(),
                dueFormat.format(passage.getDueAt()),
                late,
                responsibleName,
                fileUrl(file.getId()));
    }

    private EmailMessageModel buildAlertModel(Alert alert, String intendedTo) {
        AlertType type = alert.getAlertType();
        FileEntity file = alert.getFile();
        FilePassage passage = alert.getFilePassage();
        User recipient = alert.getRecipient();
        Instant now = clockService.now();

        String templateCode = type != null ? type.getEmailTemplateCode() : null;
        String tone = toneFor(templateCode);
        String intro = introFor(templateCode, type);

        EmailMessageModel.Builder b = EmailMessageModel.builder()
                .productName(tenantSettingsService.productName())
                .tenantBadge(tenantSettingsService.current().getBadge())
                .alertLabel(type != null ? type.getLabel() : "Alerte")
                .alertDescription(type != null ? type.getDescription() : null)
                .intro(intro)
                .tone(tone)
                .recipientFirstName(recipient.getFirstName())
                .escalationLevel(alert.getEscalationLevel())
                .redirectNotice(redirectNotice(intendedTo));

        if (file != null) {
            b.fileReference(file.getReferenceNumber())
                    .fileSubject(file.getSubject())
                    .fileUrl(fileUrl(file.getId()));
        }
        if (passage != null) {
            b.stepLabel(passage.getChainStepTemplate().getLabel());
            if (passage.getDueAt() != null) {
                DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
                        .withZone(tenantSettingsService.zoneId());
                b.dueAtFormatted(fmt.format(passage.getDueAt()));
                if (passage.getDueAt().isBefore(now)) {
                    b.overdueWorkingDays(delaiService.countWorkingDays(passage.getDueAt(), now));
                }
            }
            if (passage.getResponsibleUser() != null) {
                b.responsibleName(displayName(passage.getResponsibleUser()));
            }
        }
        return b.build();
    }

    private void sendHtml(String intendedTo, String subject, String html) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
            helper.setFrom(tenantSettingsService.fromAddress());
            helper.setTo(resolveTo(intendedTo));
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(message);
        } catch (MessagingException e) {
            throw new IllegalStateException("Failed to build email message: " + e.getMessage(), e);
        }
    }

    private String resolveTo(String intendedTo) {
        String redirectTo = tenantSettingsService.emailRedirectTo();
        if (redirectTo.isEmpty()) {
            return intendedTo;
        }
        log.info("ALR: redirection email {} → {}", intendedTo, redirectTo);
        return redirectTo;
    }

    private String redirectNotice(String intendedTo) {
        String redirectTo = tenantSettingsService.emailRedirectTo();
        if (redirectTo.isEmpty() || intendedTo == null || intendedTo.equalsIgnoreCase(redirectTo)) {
            return null;
        }
        return "Environnement de test — destinataire prévu : " + intendedTo;
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

    private String fileUrl(java.util.UUID fileId) {
        if (fileId == null) {
            return trimSlash(appBaseUrl);
        }
        return trimSlash(appBaseUrl) + "/files/" + fileId;
    }

    private static String trimSlash(String url) {
        if (url == null || url.isBlank()) {
            return "http://localhost:3000";
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url.trim();
    }

    private static String displayName(User user) {
        return (user.getFirstName() + " " + user.getLastName()).trim();
    }

    private static String toneFor(String templateCode) {
        if (templateCode == null) {
            return "navy";
        }
        return switch (templateCode.trim().toLowerCase(Locale.ROOT)) {
            case "alert-reminder" -> "teal";
            case "alert-overdue" -> "amber";
            case "alert-escalation" -> "crimson";
            case "passage-arrival" -> "forest";
            case "passage-cc" -> "slate";
            case "alert-daily-digest" -> "navy";
            default -> "navy";
        };
    }

    private static String introFor(String templateCode, AlertType type) {
        String code = templateCode == null ? "" : templateCode.trim().toLowerCase(Locale.ROOT);
        return switch (code) {
            case "alert-reminder" ->
                    "Un rappel vous est adressé avant l'échéance du maillon en cours. Merci d'anticiper le traitement.";
            case "alert-overdue" ->
                    "L'échéance du maillon est dépassée. Merci de traiter ce dossier en priorité.";
            case "alert-escalation" ->
                    "Cette alerte a été escaladée au niveau hiérarchique configuré. Une action de supervision est attendue.";
            case "passage-arrival" ->
                    "Un dossier vient d'arriver sur votre maillon. Vous en êtes le responsable du traitement.";
            case "passage-cc" ->
                    "Vous êtes en copie informée sur ce maillon. Aucune action de transmission ne vous est demandée.";
            default -> type != null && type.getDescription() != null && !type.getDescription().isBlank()
                    ? type.getDescription()
                    : "Une notification relative à un dossier vous a été adressée.";
        };
    }

    /** Utilitaire tests / construction liste digest côté moteur. */
    public List<EmailDigestItem> buildDigestItems(List<FilePassage> passages, Instant now) {
        List<EmailDigestItem> items = new ArrayList<>(passages.size());
        for (FilePassage passage : passages) {
            items.add(toDigestItem(passage, now));
        }
        return items;
    }
}
