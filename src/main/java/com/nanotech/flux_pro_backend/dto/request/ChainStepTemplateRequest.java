package com.nanotech.flux_pro_backend.dto.request;

import com.nanotech.flux_pro_backend.enumeration.DelayUnit;
import com.nanotech.flux_pro_backend.enumeration.UserRole;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ChainStepTemplateRequest(
        java.util.UUID id,
        @NotNull @Min(1) Integer stepOrder,
        @NotBlank @Size(max = 255) String label,
        @NotNull UserRole responsibleRole,
        @NotNull @Min(0) Integer delayValue,
        @NotNull DelayUnit delayUnit,
        @Size(max = 500) String expectedAction,
        boolean optional,
        boolean closureStep) {
}
