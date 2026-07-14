package com.nanotech.flux_pro_backend.dto.response;

import com.nanotech.flux_pro_backend.enumeration.DelayUnit;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ChainTemplateDetailResponse(
        UUID id,
        String code,
        String name,
        String description,
        String fileTypeCode,
        int totalDelayDays,
        DelayUnit delayUnit,
        boolean active,
        boolean systemTemplate,
        boolean linkedToFiles,
        Instant createdAt,
        Instant updatedAt,
        List<ChainStepTemplateResponse> steps) {
}
