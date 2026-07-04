package com.nanotech.flux_pro_backend.service;

import com.nanotech.flux_pro_backend.common.AlertException;
import com.nanotech.flux_pro_backend.dto.request.AlertRuleRequest;
import com.nanotech.flux_pro_backend.entity.AlertRule;
import com.nanotech.flux_pro_backend.entity.AlertType;
import com.nanotech.flux_pro_backend.entity.ChainStepTemplate;
import com.nanotech.flux_pro_backend.entity.ChainTemplate;
import com.nanotech.flux_pro_backend.enumeration.AlertTargetMode;
import com.nanotech.flux_pro_backend.repository.AlertRuleRepository;
import com.nanotech.flux_pro_backend.repository.ChainStepTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Gestion des règles d'alerte par template (ALR-06). Chaque règle appartient obligatoirement
 * à un ChainTemplate : il n'existe aucune règle globale/implicite (cf. docs/SPEC-ALR.md §7 ALR-R04).
 */
@Service
@RequiredArgsConstructor
public class AlertRuleService {

    private final AlertRuleRepository alertRuleRepository;
    private final ChainStepTemplateRepository chainStepTemplateRepository;
    private final ChainTemplateService chainTemplateService;
    private final AlertTypeService alertTypeService;
    private final AlertRuleSeedProfileService alertRuleSeedProfileService;

    @Transactional(readOnly = true)
    public List<AlertRule> listByTemplate(UUID chainTemplateId) {
        chainTemplateService.findById(chainTemplateId);
        return alertRuleRepository.findByChainTemplateId(chainTemplateId);
    }

    @Transactional(readOnly = true)
    public AlertRule getById(UUID chainTemplateId, UUID ruleId) {
        return alertRuleRepository.findByIdAndChainTemplateId(ruleId, chainTemplateId)
                .orElseThrow(() -> AlertException.notFound("ALERT_RULE_NOT_FOUND", "Alert rule not found"));
    }

    @Transactional
    public AlertRule create(UUID chainTemplateId, AlertRuleRequest request) {
        ChainTemplate template = chainTemplateService.findById(chainTemplateId);
        AlertRule rule = new AlertRule();
        rule.setChainTemplate(template);
        applyRequest(rule, template, request);
        return alertRuleRepository.save(rule);
    }

    @Transactional
    public AlertRule update(UUID chainTemplateId, UUID ruleId, AlertRuleRequest request) {
        AlertRule rule = getById(chainTemplateId, ruleId);
        applyRequest(rule, rule.getChainTemplate(), request);
        return alertRuleRepository.save(rule);
    }

    @Transactional
    public AlertRule activate(UUID chainTemplateId, UUID ruleId) {
        AlertRule rule = getById(chainTemplateId, ruleId);
        rule.setActive(true);
        return alertRuleRepository.save(rule);
    }

    @Transactional
    public AlertRule deactivate(UUID chainTemplateId, UUID ruleId) {
        AlertRule rule = getById(chainTemplateId, ruleId);
        rule.setActive(false);
        return alertRuleRepository.save(rule);
    }

    @Transactional
    public void delete(UUID chainTemplateId, UUID ruleId) {
        AlertRule rule = getById(chainTemplateId, ruleId);
        alertRuleRepository.delete(rule);
    }

    @Transactional
    public List<AlertRule> applyDefaultProfile(UUID chainTemplateId, boolean overwriteExisting) {
        ChainTemplate template = chainTemplateService.findById(chainTemplateId);
        return alertRuleSeedProfileService.apply(template, overwriteExisting);
    }

    private void applyRequest(AlertRule rule, ChainTemplate template, AlertRuleRequest request) {
        if (request.targetMode() == AlertTargetMode.ROLE && request.targetRole() == null) {
            throw AlertException.badRequest(
                    "ALERT_RULE_TARGET_ROLE_REQUIRED", "targetRole is required when targetMode = ROLE");
        }
        AlertType alertType = alertTypeService.getById(request.alertTypeId());
        ChainStepTemplate step = null;
        if (request.chainStepTemplateId() != null) {
            step = chainStepTemplateRepository.findByIdAndChainTemplateId(request.chainStepTemplateId(), template.getId())
                    .orElseThrow(() -> AlertException.badRequest(
                            "ALERT_RULE_STEP_INVALID", "Chain step template does not belong to this chain template"));
        }
        rule.setChainStepTemplate(step);
        rule.setThresholdCode(request.thresholdCode().trim().toUpperCase());
        rule.setOffsetValue(request.offsetValue());
        rule.setOffsetUnit(request.offsetUnit());
        rule.setAlertType(alertType);
        rule.setEscalationLevel(request.escalationLevel());
        rule.setTargetMode(request.targetMode());
        rule.setTargetRole(request.targetMode() == AlertTargetMode.ROLE ? request.targetRole() : null);
        rule.setPriorityScope(
                request.priorityScope() != null && !request.priorityScope().isBlank()
                        ? request.priorityScope().trim().toUpperCase()
                        : null);
        rule.setActive(request.active());
    }
}
