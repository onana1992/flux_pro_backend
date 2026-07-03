package com.nanotech.flux_pro_backend.dto.response;

import java.util.UUID;

public record FileTypeResponse(
        UUID id,
        String code,
        String name,
        String nameEn,
        String description,
        String directionCode,
        int sortOrder,
        boolean active) {
}
