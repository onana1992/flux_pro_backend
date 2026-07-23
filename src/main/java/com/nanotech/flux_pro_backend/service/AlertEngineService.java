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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
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
@Slf4j
public class AlertEngineService {

    private final FilePassageRepository filePassageRepository;
    private final AlertRuleRepository alertRuleRepository;
    private final AlertRepository alertRepository;
    private final DelaiService delaiService;
    private final ClockService clockService;
    private final ResponsibleUserResolver responsibleUserResolver;
    private final SubstituteService substituteService;
    private final NotificationService notificationService;
    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final AlertDigestRecipientRoleService digestRecipientRoleService;
    private final TransactionTemplate requiresNewTx;

    public AlertEngineService(
            FilePassageRepository filePassageRepository,
            AlertRuleRepository alertRuleRepository,
            AlertRepository alertRepository,
            DelaiService delaiService,
            ClockService clockService,
            ResponsibleUserResolver responsibleUserResolver,
            SubstituteService substituteService,
            NotificationService notificationService,
            OrganizationRepository organizationRepository,
            UserRepository userRepository,
            EmailService emailService,
            AlertDigestRecipientRoleService digestRecipientRoleService,
            PlatformTransactionManager transactionManager) {
        this.filePassageRepository = filePassageRepository;
        this.alertRuleRepository = alertRuleRepository;
        this.alertRepository = alertRepository;
        this.delaiService = delaiService;
        this.clockService = clockService;
        this.responsibleUserResolver = responsibleUserResolver;
        this.substituteService = substituteService;
        this.notificationService = notificationService;
        this.organizationRepository = organizationRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.digestRecipientRoleService = digestRecipientRoleService;
        this.requiresNewTx = new TransactionTemplate(transactionManager);
        this.requiresNewTx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    /**
     * Pas de transaction englobante : chaque passage est évalué en REQUIRES_NEW
     * pour qu'un échec (SMTP, règle) ne rollback pas les alertes des autres.
     */
    public void evaluateAll() {
        Instant now = clockService.now();
        List<FilePassage> candidates = filePassageRepository.findActiveCandidatesForAlerts();
        for (FilePassage passage : candidates) {
            UUID passageId = passage.getId();
            try {
                requiresNewTx.executeWithoutResult(status -> evaluatePassageById(passageId, now));
            } catch (Exception e) {
                log.error("ALR: échec d'évaluation du passage {} : {}", passageId, e.getMessage(), e);
            }
        }
    }

    private void evaluatePassageById(UUID passageId, Instant now) {
        FilePassage passage = filePassageRepository.findById(passageId).orElse(null);
        if (passage == null) {
            return;
        }
        evaluatePassage(passage, now);
    }

    private void evaluatePassage(FilePassage passage, Instant now) {
        FileEntity file = passage.getFile();
        ChainTemplate template = file.getChainTemplate();
        if (template == null) {
            return;
        }
        List<AlertRule> rules = alertRuleRepository.findByChainTemplateIdAndActiveTrue(template.getId());
        for (AlertRule rule : rules) {
            try {
                if (!appliesToStep(rule, passage) || !appliesToPriority(rule, file)) {
                    continue;
                }
                Instant threshold = delaiService.applyOffset(
                        passage.getDueAt(), rule.getOffsetValue(), rule.getOffsetUnit());
                if (now.isBefore(threshold)) {
                    continue;
                }
                User recipient = resolveRecipient(rule, passage, file);
                if (recipient == null) {
                    log.warn(
                            "ALR: aucun destinataire résolu pour la règle {} (passage {})",
                            rule.getId(),
                            passage.getId());
                    continue;
                }
                triggerAlert(passage, file, rule, recipient);
            } catch (Exception e) {
                log.error(
                        "ALR: règle {} échouée pour passage {} : {}",
                        rule.getId(),
                        passage.getId(),
                        e.getMessage(),
                        e);
            }
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
        User recipient;
        if (rule.getTargetMode() == AlertTargetMode.CURRENT_RESPONSIBLE) {
            recipient = passage.getResponsibleUser();
        } else {
            recipient = responsibleUserResolver.resolve(file, rule.getTargetRole());
        }
        return substituteService.effectiveRecipient(recipient);
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
     * Digest quotidien des retards (ALR-08). Les rôles destinataires sont lus en base
     * ({@link AlertDigestRecipientRoleService}), configurables via l'UI Paramètres —
     * jamais un rôle unique figé dans le code.
     */
    @Transactional(readOnly = true)
    public void runDailyDigest() {
        List<UserRole> digestRoles = digestRecipientRoleService.listRoles();
        if (digestRoles.isEmpty()) {
            log.info("ALR: digest ignoré — aucun rôle destinataire configuré");
            return;
        }
        Set<UserRole> roleSet = Set.copyOf(digestRoles);
        Instant now = clockService.now();
        List<FilePassage> overduePassages = filePassageRepository.findOverdueForDigest(now);
        if (overduePassages.isEmpty()) {
            return;
        }
        List<User> recipients = userRepository.findAll().stream()
                .filter(User::isActive)
                .filter(u -> roleSet.contains(u.getRole()))
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
                        recipient.getFirstName(),
                        emailService.buildDigestItems(scoped, now));
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
}
