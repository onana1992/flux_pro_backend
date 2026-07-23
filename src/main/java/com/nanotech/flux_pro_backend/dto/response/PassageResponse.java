package com.nanotech.flux_pro_backend.dto.response;

import com.nanotech.flux_pro_backend.enumeration.DelayUnit;
import com.nanotech.flux_pro_backend.enumeration.PassageStatus;
import com.nanotech.flux_pro_backend.enumeration.UserRole;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PassageResponse(
        UUID id,
        int stepOrder,
        String label,
        String expectedAction,
        boolean optional,
        boolean closureStep,
        UserRole responsibleRole,
        int delayValue,
        DelayUnit delayUnit,
        PassageStatus status,
        UUID responsibleUserId,
        String responsibleName,
        String responsibleEmail,
        String responsiblePhone,
        String responsibleJobTitle,
        String responsibleOrganizationCode,
        String responsibleOrganizationName,
        Instant receivedAt,
        Instant transmittedAt,
        Instant dueAt,
        BigDecimal consumedHours,
        Integer workingDaysHeld,
        boolean overdue,
        String comment,
        String internalComment,
        String returnReason,
        Instant suspendedAt,
        Instant resumedAt,
        List<PassageCcUserResponse> ccUsers
) {
}
