package com.nanotech.flux_pro_backend.mapper;

import com.nanotech.flux_pro_backend.dto.response.AlertRuleResponse;
import com.nanotech.flux_pro_backend.entity.AlertRule;

public final class AlertRuleMapper {

    private AlertRuleMapper() {
    }

    public static AlertRuleResponse toResponse(AlertRule rule) {
        return new AlertRuleResponse(
                rule.getId(),
                rule.getChainTemplate().getId(),
                rule.getChainStepTemplate() != null ? rule.getChainStepTemplate().getId() : null,
                rule.getChainStepTemplate() != null ? rule.getChainStepTemplate().getLabel() : null,
                rule.getThresholdCode(),
                rule.getOffsetValue(),
                rule.getOffsetUnit(),
                rule.getAlertType().getId(),
                rule.getAlertType().getCode(),
                rule.getAlertType().getLabel(),
                rule.getEscalationLevel(),
                rule.getTargetMode(),
                rule.getTargetRole(),
                rule.getPriorityScope(),
                rule.isActive());
    }
}
