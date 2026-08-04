package com.nanotech.flux_pro_backend.dto.response;

import java.util.List;
import java.util.UUID;

public record AssistantCitationResponse(
        String type,
        UUID id,
        String label,
        String href
) {
}
