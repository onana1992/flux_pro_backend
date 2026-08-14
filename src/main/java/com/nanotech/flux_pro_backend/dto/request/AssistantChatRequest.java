package com.nanotech.flux_pro_backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record AssistantChatRequest(
        UUID conversationId,
        @NotBlank @Size(max = 4000) String message,
        @Size(max = 10) String locale
) {
}
