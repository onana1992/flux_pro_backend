package com.nanotech.flux_pro_backend.service;

import com.nanotech.flux_pro_backend.entity.Alert;
import com.nanotech.flux_pro_backend.entity.AlertRule;
import com.nanotech.flux_pro_backend.entity.ChainTemplate;
import com.nanotech.flux_pro_backend.entity.FileEntity;
import com.nanotech.flux_pro_backend.entity.FilePassage;
import com.nanotech.flux_pro_backend.entity.Organization;
import com.nanotech.flux_pro_backend.entity.User;
import com.nanotech.flux_pro_backend.enumeration.AlertChannel;
import com.nanotech.flux_pro_backend.enumeration.AlertStatus;
import com.nanotech.flux_pro_backend.enumeration.AlertTargetMode;
import com.nanotech.flux_pro_backend.enumeration.FilePriority;
import com.nanotech.flux_pro_backend.enumeration.UserRole;
import com.nanotech.flux_pro_backend.repository.AlertRepository;
import com.nanotech.flux_pro_backend.repository.AlertRuleRepository;
import com.nanotech.flux_pro_backend.repository.FilePassageRepository;
import com.nanotech.flux_pro_backend.repository.OrganizationRepository;
import com.nanotech.flux_pro_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Moteur d'alertes (ALR-01 à ALR-04) — 100 % piloté par la configuration :
 * les règles à évaluer viennent uniquement de AlertRule.chainTemplate (jamais de règle
 * globale/implicite, cf. docs/SPEC-ALR.md §7 ALR-R04) et le destinataire est résolu via
 * AlertRule.targetMode / targetRole (jamais un rôle codé en dur, cf. §8.3).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AlertEngineService {

    private static final DateTimeFormatter DUE_DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy").withZone(ZoneId.of("Africa/Douala"));

    private final FilePassageRepository filePassageRepository;
    private final AlertRuleRepository alertRuleRepository;
    private final AlertRepository alertRepository;
    private final DelaiService delaiService;
    private final ResponsibleUserResolver responsibleUserResolver;
    private final NotificationService notificationService;
    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    @Transactional
    public void evaluateAll() {
        Instant now = Instant.now();
        List<FilePassage> candidates = filePassageRepository.findActiveCandidatesForAlerts();
        for (FilePassage passage : candidates) {
            evaluatePassage(passage, now);
        }
    }

    private void evaluatePassage(FilePassage passage, Instant now) {
        FileEntity file = passage.getFile();
        ChainTemplate template = file.getChainTemplate();
        if (template == null) {
            return;
        }
        List<AlertRule> rules = alertRuleRepository.findByChainTemplateIdAndActiveTrue(template.getId());
        for (AlertRule rule : rules) {
            if (!appliesToStep(rule, passage) || !appliesToPriority(rule, file)) {
                continue;
            }
            Instant threshold = delaiService.applyOffset(passage.getDueAt(), rule.getOffsetValue(), rule.getOffsetUnit());
            if (now.isBefore(threshold)) {
                continue;
            }
            User recipient = resolveRecipient(rule, passage, file);
            if (recipient == null) {
                log.warn("ALR: aucun destinataire résolu pour la règle {} (passage {})", rule.getId(), passage.getId());
                continue;
            }
            triggerAlert(passage, file, rule, recipient);
        }
    }

    private boolean appliesToStep(AlertRule rule, FilePassage passage) {
        return rule.getChainStepTemplate() == null
                || rule.getChainStepTemplate().getId().equals(passage.getChainStepTemplate().getId());
    }

    private boolean appliesToPriority(AlertRule rule, FileEntity file) {
        if (rule.getPriorityScope() == null) {
            return true;
        }
        if ("URGENT_PLUS".equalsIgnoreCase(rule.getPriorityScope())) {
            return file.getPriority() != FilePriority.NORMAL;
        }
        return true;
    }

    private User resolveRecipient(AlertRule rule, FilePassage passage, FileEntity file) {
        if (rule.getTargetMode() == AlertTargetMode.CURRENT_RESPONSIBLE) {
            return passage.getResponsibleUser();
        }
        return responsibleUserResolver.resolve(file, rule.getTargetRole());
    }

    private void triggerAlert(FilePassage passage, FileEntity file, AlertRule rule, User recipient) {
        for (AlertChannel channel : notificationService.activeChannels()) {
            if (alertRepository.existsByFilePassageIdAndAlertRuleIdAndChannel(passage.getId(), rule.getId(), channel)) {
                continue;
            }
            Alert alert = new Alert();
            alert.setFile(file);
            alert.setFilePassage(passage);
            alert.setAlertRule(rule);
            alert.setAlertType(rule.getAlertType());
            alert.setEscalationLevel(rule.getEscalationLevel());
            alert.setChannel(channel);
            alert.setRecipient(recipient);
            alert.setStatus(AlertStatus.PENDING);
            alert = alertRepository.save(alert);
            notificationService.dispatch(alert);
        }
    }

    /**
     * Digest quotidien des retards (ALR-08). Le rôle destinataire est une propriété
     * (fluxpro.alerts.digest.target-role, DIRECTOR par défaut), pas un rôle codé en dur dans
     * cette méthode : n'importe quelle valeur de UserRole peut être configurée sans recompiler.
     */
    @Transactional(readOnly = true)
    public void runDailyDigest(UserRole digestRole) {
        Instant now = Instant.now();
        List<FilePassage> overduePassages = filePassageRepository.findOverdueForDigest(now);
        if (overduePassages.isEmpty()) {
            return;
        }
        List<User> recipients = userRepository.findAll().stream()
                .filter(User::isActive)
                .filter(u -> u.getRole() == digestRole)
                .toList();
        for (User recipient : recipients) {
            Set<UUID> scope = collectOrganizationSubtree(recipient.getOrganization().getId());
            List<FilePassage> scoped = overduePassages.stream()
                    .filter(p -> scope.contains(p.getFile().getOrganization().getId()))
                    .sorted(Comparator.comparing(FilePassage::getDueAt))
                    .toList();
            if (scoped.isEmpty()) {
                continue;
            }
            try {
                emailService.sendDigest(
                        recipient.getEmail(),
                        "[ChaîneFlux] Récapitulatif quotidien des retards (" + scoped.size() + ")",
                        buildDigestBody(scoped, now));
            } catch (Exception e) {
                log.warn("ALR: digest échoué pour {} : {}", recipient.getEmail(), e.getMessage());
            }
        }
    }

    private Set<UUID> collectOrganizationSubtree(UUID rootOrgId) {
        Set<UUID> ids = new HashSet<>();
        ids.add(rootOrgId);
        collectDescendants(rootOrgId, ids);
        return ids;
    }

    private void collectDescendants(UUID orgId, Set<UUID> ids) {
        for (Organization child : organizationRepository.findByParentId(orgId)) {
            if (ids.add(child.getId())) {
                collectDescendants(child.getId(), ids);
            }
        }
    }

    private String buildDigestBody(List<FilePassage> scoped, Instant now) {
        StringBuilder sb = new StringBuilder("Dossiers en retard :\n\n");
        for (FilePassage passage : scoped) {
            FileEntity file = passage.getFile();
            int workingDaysLate = delaiService.countWorkingDays(passage.getDueAt(), now);
            sb.append("- ").append(file.getReferenceNumber())
                    .append(" — ").append(file.getSubject())
                    .append(" — étape : ").append(passage.getChainStepTemplate().getLabel())
                    .append(" — échéance : ").append(DUE_DATE_FORMAT.format(passage.getDueAt()))
                    .append(" — retard : ").append(workingDaysLate).append(" j. ouvré(s)\n");
        }
        sb.append("\nConnectez-vous à ChaîneFlux pour plus de détails.");
        return sb.toString();
    }
}
