package com.nanotech.flux_pro_backend.dto.response;

import com.nanotech.flux_pro_backend.enumeration.DelayUnit;
import com.nanotech.flux_pro_backend.enumeration.UserRole;

import java.util.UUID;

public record ChainStepTemplateResponse(
        UUID id,
        int stepOrder,
        String label,
        UserRole responsibleRole,
        UUID organizationId,
        String organizationCode,
        String organizationName,
        int delayValue,
        DelayUnit delayUnit,
        String expectedAction,
        boolean optional,
        boolean closureStep) {
}
