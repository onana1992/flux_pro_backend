package com.nanotech.flux_pro_backend.mapper;

import com.nanotech.flux_pro_backend.dto.response.ChainStepTemplateResponse;
import com.nanotech.flux_pro_backend.dto.response.ChainTemplateDetailResponse;
import com.nanotech.flux_pro_backend.dto.response.ChainTemplateSummaryResponse;
import com.nanotech.flux_pro_backend.entity.ChainStepTemplate;
import com.nanotech.flux_pro_backend.entity.ChainTemplate;

public final class ChainTemplateMapper {

    private ChainTemplateMapper() {
    }

    public static ChainTemplateSummaryResponse toSummary(ChainTemplate template) {
        return new ChainTemplateSummaryResponse(
                template.getId(),
                template.getCode(),
                template.getName(),
                template.getFileTypeCode(),
                template.getTotalDelayDays(),
                template.getDelayUnit(),
                template.isActive(),
                template.isSystemTemplate(),
                template.getSteps() != null ? template.getSteps().size() : 0);
    }

    public static ChainTemplateDetailResponse toDetail(ChainTemplate template) {
        return toDetail(template, false);
    }

    public static ChainTemplateDetailResponse toDetail(ChainTemplate template, boolean linkedToFiles) {
        return new ChainTemplateDetailResponse(
                template.getId(),
                template.getCode(),
                template.getName(),
                template.getDescription(),
                template.getFileTypeCode(),
                template.getTotalDelayDays(),
                template.getDelayUnit(),
                template.isActive(),
                template.isSystemTemplate(),
                linkedToFiles,
                template.getCreatedAt(),
                template.getUpdatedAt(),
                template.getSteps().stream().map(ChainTemplateMapper::toStepResponse).toList());
    }

    public static ChainStepTemplateResponse toStepResponse(ChainStepTemplate step) {
        return new ChainStepTemplateResponse(
                step.getId(),
                step.getStepOrder(),
                step.getLabel(),
                step.getResponsibleRole(),
                step.getDelayValue(),
                step.getDelayUnit(),
                step.getExpectedAction(),
                step.isOptional(),
                step.isClosureStep());
    }
}
