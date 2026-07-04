package com.nanotech.flux_pro_backend.dto.request;

import com.nanotech.flux_pro_backend.enumeration.AlertTargetMode;
import com.nanotech.flux_pro_backend.enumeration.DelayUnit;
import com.nanotech.flux_pro_backend.enumeration.UserRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * chainTemplateId n'est PAS un champ ici : il vient du chemin de l'URL
 * (/api/admin/chain-templates/{chainTemplateId}/alert-rules), une règle appartenant
 * toujours à un seul template (ALR-06, cf. docs/SPEC-ALR.md §6.3).
 */
public record AlertRuleRequest(
        UUID chainStepTemplateId,
        @NotBlank @Size(max = 20) String thresholdCode,
        @NotNull Integer offsetValue,
        @NotNull DelayUnit offsetUnit,
        @NotNull UUID alertTypeId,
        Integer escalationLevel,
        @NotNull AlertTargetMode targetMode,
        UserRole targetRole,
        @Size(max = 20) String priorityScope,
        @NotNull Boolean active) {
}
