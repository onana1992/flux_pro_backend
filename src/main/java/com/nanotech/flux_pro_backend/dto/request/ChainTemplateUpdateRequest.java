package com.nanotech.flux_pro_backend.dto.request;

import com.nanotech.flux_pro_backend.enumeration.DelayUnit;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ChainTemplateUpdateRequest(
        @NotBlank @Size(max = 255) String name,
        String description,
        @Size(max = 32) String fileTypeCode,
        @NotNull @Min(0) Integer totalDelayDays,
        @NotNull DelayUnit delayUnit) {
}
