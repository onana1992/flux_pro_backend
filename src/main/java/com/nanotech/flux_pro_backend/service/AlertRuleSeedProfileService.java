package com.nanotech.flux_pro_backend.service;

import com.nanotech.flux_pro_backend.common.AlertException;
import com.nanotech.flux_pro_backend.entity.AlertRule;
import com.nanotech.flux_pro_backend.entity.AlertType;
import com.nanotech.flux_pro_backend.entity.ChainTemplate;
import com.nanotech.flux_pro_backend.enumeration.AlertTargetMode;
import com.nanotech.flux_pro_backend.enumeration.DelayUnit;
import com.nanotech.flux_pro_backend.enumeration.UserRole;
import com.nanotech.flux_pro_backend.repository.AlertRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Profil de seed CDC §10.2 — jamais lu directement par le moteur d'alertes (ALR-F18).
 * Il ne s'agit que d'un modèle que l'admin métier peut appliquer (copier) explicitement
 * sur un template précis, comme point de départ éditable ensuite ligne par ligne
 * (cf. docs/SPEC-ALR.md §6.4).
 */
@Service
@RequiredArgsConstructor
public class AlertRuleSeedProfileService {

    private record SeedRow(
            String thresholdCode,
            int offsetValue,
            DelayUnit offsetUnit,
            String alertTypeCode,
            Integer escalationLevel,
            AlertTargetMode targetMode,
            UserRole targetRole,
            String priorityScope) {
    }

    private static final List<SeedRow> DEFAULT_PROFILE = List.of(
            new SeedRow("J_MINUS_2", -2, DelayUnit.WORKING_DAYS, "REMINDER", null,
                    AlertTargetMode.CURRENT_RESPONSIBLE, null, null),
            new SeedRow("J_PLUS_0", 0, DelayUnit.WORKING_DAYS, "OVERDUE", null,
                    AlertTargetMode.CURRENT_RESPONSIBLE, null, null),
            new SeedRow("J_PLUS_0", 0, DelayUnit.WORKING_DAYS, "OVERDUE", null,
                    AlertTargetMode.ROLE, UserRole.SERVICE_HEAD, null),
            new SeedRow("J_PLUS_3", 3, DelayUnit.WORKING_DAYS, "ESCALATION", 1,
                    AlertTargetMode.ROLE, UserRole.DIRECTOR, null),
            new SeedRow("J_PLUS_7", 7, DelayUnit.WORKING_DAYS, "ESCALATION", 2,
                    AlertTargetMode.ROLE, UserRole.SECRETARY_GENERAL, null),
            new SeedRow("J_PLUS_15", 15, DelayUnit.WORKING_DAYS, "ESCALATION", 3,
                    AlertTargetMode.ROLE, UserRole.EXECUTIVE_OFFICE, "URGENT_PLUS"));

    private final AlertRuleRepository alertRuleRepository;
    private final AlertTypeService alertTypeService;

    @Transactional
    public List<AlertRule> apply(ChainTemplate template, boolean overwriteExisting) {
        if (overwriteExisting) {
            alertRuleRepository.deleteAllByChainTemplateId(template.getId());
        }
        List<AlertRule> created = new ArrayList<>();
        for (SeedRow row : DEFAULT_PROFILE) {
            boolean exists = alertRuleRepository.existsByChainTemplateIdAndThresholdCodeAndTargetModeAndTargetRole(
                    template.getId(), row.thresholdCode(), row.targetMode(), row.targetRole());
            if (exists) {
                continue;
            }
            AlertType alertType = resolveAlertType(row.alertTypeCode());
            AlertRule rule = new AlertRule();
            rule.setChainTemplate(template);
            rule.setThresholdCode(row.thresholdCode());
            rule.setOffsetValue(row.offsetValue());
            rule.setOffsetUnit(row.offsetUnit());
            rule.setAlertType(alertType);
            rule.setEscalationLevel(row.escalationLevel());
            rule.setTargetMode(row.targetMode());
            rule.setTargetRole(row.targetRole());
            rule.setPriorityScope(row.priorityScope());
            rule.setActive(true);
            created.add(alertRuleRepository.save(rule));
        }
        return created;
    }

    private AlertType resolveAlertType(String code) {
        try {
            return alertTypeService.getByCode(code);
        } catch (AlertException e) {
            throw AlertException.conflict(
                    "ALERT_TYPE_SEED_MISSING",
                    "Seed alert type not found: " + code
                            + " — execute docs/sql/2026-07-04_alert_types.sql before applying the default profile",
                    code);
        }
    }
}
