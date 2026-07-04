package com.nanotech.flux_pro_backend.dto.response;

import java.util.UUID;

public record AlertTypeResponse(
        UUID id,
        String code,
        String label,
        String description,
        String emailTemplateCode,
        boolean systemDefined,
        boolean active) {
}
