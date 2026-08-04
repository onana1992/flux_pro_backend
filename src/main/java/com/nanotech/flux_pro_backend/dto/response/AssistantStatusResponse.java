package com.nanotech.flux_pro_backend.dto.response;

public record AssistantStatusResponse(
        boolean enabled,
        boolean llmConfigured
) {
}
