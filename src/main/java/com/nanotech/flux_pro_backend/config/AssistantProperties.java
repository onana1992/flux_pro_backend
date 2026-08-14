package com.nanotech.flux_pro_backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "fluxpro.assistant")
public record AssistantProperties(
        boolean enabled,
        int timeoutSeconds,
        int maxHistoryMessages,
        int maxToolCalls
) {
    public AssistantProperties {
        if (timeoutSeconds <= 0) {
            timeoutSeconds = 60;
        }
        if (maxHistoryMessages <= 0) {
            maxHistoryMessages = 10;
        }
        if (maxToolCalls <= 0) {
            maxToolCalls = 5;
        }
    }
}
