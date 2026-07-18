package com.nanotech.flux_pro_backend.dto.request;

import com.nanotech.flux_pro_backend.enumeration.ClockAdjustUnit;
import jakarta.validation.constraints.NotNull;

public record ClockAdjustRequest(
        @NotNull Integer amount,
        @NotNull ClockAdjustUnit unit
) {
}
