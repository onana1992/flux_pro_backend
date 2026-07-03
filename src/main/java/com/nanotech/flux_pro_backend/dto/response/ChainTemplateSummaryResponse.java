package com.nanotech.flux_pro_backend.dto.response;

import com.nanotech.flux_pro_backend.enumeration.DelayUnit;

import java.util.UUID;

public record ChainTemplateSummaryResponse(
        UUID id,
        String code,
        String name,
        String fileTypeCode,
        int totalDelayDays,
        DelayUnit delayUnit,
        boolean active,
        boolean systemTemplate,
        int stepCount) {
}
