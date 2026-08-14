package com.nanotech.flux_pro_backend.dto.response;

import java.util.List;
import java.util.UUID;

public record AssistantChatResponse(
        UUID conversationId,
        UUID messageId,
        String answer,
        List<AssistantCitationResponse> citations,
        List<String> toolsUsed,
        boolean refused
) {
}
