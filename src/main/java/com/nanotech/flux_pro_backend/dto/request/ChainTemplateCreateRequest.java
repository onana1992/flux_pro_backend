package com.nanotech.flux_pro_backend.dto.request;

import com.nanotech.flux_pro_backend.enumeration.DelayUnit;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ChainTemplateCreateRequest(
        @NotBlank @Size(max = 10) String code,
        @NotBlank @Size(max = 255) String name,
        String description,
        @Size(max = 32) String fileTypeCode,
        @NotNull @Min(0) Integer totalDelayDays,
        @NotNull DelayUnit delayUnit,
        @NotEmpty @Valid List<ChainStepTemplateRequest> steps) {
}
