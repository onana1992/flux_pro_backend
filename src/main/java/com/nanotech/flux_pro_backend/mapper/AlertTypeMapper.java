package com.nanotech.flux_pro_backend.mapper;

import com.nanotech.flux_pro_backend.dto.response.AlertTypeResponse;
import com.nanotech.flux_pro_backend.entity.AlertType;

public final class AlertTypeMapper {

    private AlertTypeMapper() {
    }

    public static AlertTypeResponse toResponse(AlertType type) {
        return new AlertTypeResponse(
                type.getId(),
                type.getCode(),
                type.getLabel(),
                type.getDescription(),
                type.getEmailTemplateCode(),
                type.isSystemDefined(),
                type.isActive());
    }
}
