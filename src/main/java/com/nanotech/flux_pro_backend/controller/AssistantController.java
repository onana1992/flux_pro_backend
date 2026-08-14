package com.nanotech.flux_pro_backend.controller;

import com.nanotech.flux_pro_backend.config.AssistantProperties;
import com.nanotech.flux_pro_backend.dto.request.AssistantChatRequest;
import com.nanotech.flux_pro_backend.dto.response.AssistantChatResponse;
import com.nanotech.flux_pro_backend.dto.response.AssistantStatusResponse;
import com.nanotech.flux_pro_backend.security.RbacPermissions;
import com.nanotech.flux_pro_backend.security.RequiresPermission;
import com.nanotech.flux_pro_backend.security.SecurityUtils;
import com.nanotech.flux_pro_backend.service.assistant.AssistantOrchestrator;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/assistant")
@RequiredArgsConstructor
public class AssistantController {

    private final AssistantProperties properties;
    private final AssistantOrchestrator orchestrator;
    private final SecurityUtils securityUtils;
    private final ObjectProvider<ChatClient> chatClientProvider;

    @GetMapping("/status")
    public AssistantStatusResponse status() {
        securityUtils.currentUser();
        boolean llmConfigured = chatClientProvider.getIfAvailable() != null;
        return new AssistantStatusResponse(properties.enabled(), llmConfigured);
    }

    @PostMapping("/chat")
    @RequiresPermission(RbacPermissions.ASSISTANT_USE)
    public ResponseEntity<AssistantChatResponse> chat(@Valid @RequestBody AssistantChatRequest request) {
        if (!properties.enabled()) {
            return ResponseEntity.notFound().build();
        }
        AssistantChatResponse response = orchestrator.chat(request, securityUtils.currentUser());
        return ResponseEntity.ok(response);
    }
}
