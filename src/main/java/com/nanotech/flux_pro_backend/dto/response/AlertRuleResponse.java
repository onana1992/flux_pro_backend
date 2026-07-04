package com.nanotech.flux_pro_backend.dto.response;

import com.nanotech.flux_pro_backend.enumeration.AlertTargetMode;
import com.nanotech.flux_pro_backend.enumeration.DelayUnit;
import com.nanotech.flux_pro_backend.enumeration.UserRole;

import java.util.UUID;

public record AlertRuleResponse(
        UUID id,
        UUID chainTemplateId,
        UUID chainStepTemplateId,
        String chainStepTemplateLabel,
        String thresholdCode,
        int offsetValue,
        DelayUnit offsetUnit,
        UUID alertTypeId,
        String alertTypeCode,
        String alertTypeLabel,
        Integer escalationLevel,
        AlertTargetMode targetMode,
        UserRole targetRole,
        String priorityScope,
        boolean active) {
}
