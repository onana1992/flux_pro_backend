package com.nanotech.flux_pro_backend.service.assistant;

import com.nanotech.flux_pro_backend.common.AppException;
import com.nanotech.flux_pro_backend.entity.AssistantConversation;
import com.nanotech.flux_pro_backend.entity.AssistantMessage;
import com.nanotech.flux_pro_backend.entity.AssistantToolCall;
import com.nanotech.flux_pro_backend.entity.User;
import com.nanotech.flux_pro_backend.enumeration.AssistantMessageRole;
import com.nanotech.flux_pro_backend.repository.AssistantConversationRepository;
import com.nanotech.flux_pro_backend.repository.AssistantMessageRepository;
import com.nanotech.flux_pro_backend.repository.AssistantToolCallRepository;
import com.nanotech.flux_pro_backend.repository.UserRepository;
import com.nanotech.flux_pro_backend.security.SecurityUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AssistantConversationService {

    private final AssistantConversationRepository conversationRepository;
    private final AssistantMessageRepository messageRepository;
    private final AssistantToolCallRepository toolCallRepository;
    private final UserRepository userRepository;

    @Transactional
    public AssistantConversation getOrCreate(UUID conversationId, SecurityUser actor, String firstMessage) {
        if (conversationId != null) {
            return conversationRepository.findByIdAndUserId(conversationId, actor.getId())
                    .orElseThrow(() -> AppException.notFound(
                            "ASSISTANT_CONVERSATION_NOT_FOUND",
                            "Conversation introuvable"));
        }
        User user = userRepository.findById(actor.getId())
                .orElseThrow(() -> AppException.unauthorized("AUTH_USER_NOT_FOUND", "Utilisateur introuvable"));
        AssistantConversation conversation = new AssistantConversation();
        conversation.setUser(user);
        conversation.setTitle(deriveTitle(firstMessage));
        return conversationRepository.save(conversation);
    }

    @Transactional(readOnly = true)
    public List<AssistantMessage> loadHistory(UUID conversationId, int maxMessages) {
        List<AssistantMessage> all = messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);
        if (all.size() <= maxMessages) {
            return all;
        }
        return new ArrayList<>(all.subList(all.size() - maxMessages, all.size()));
    }

    @Transactional
    public AssistantMessage saveMessage(
            AssistantConversation conversation,
            AssistantMessageRole role,
            String content,
            boolean refused) {
        AssistantMessage message = new AssistantMessage();
        message.setConversation(conversation);
        message.setRole(role);
        message.setContent(content);
        message.setRefused(refused);
        AssistantMessage saved = messageRepository.save(message);
        conversation.setUpdatedAt(java.time.Instant.now());
        conversationRepository.save(conversation);
        return saved;
    }

    @Transactional
    public void saveToolCalls(AssistantMessage assistantMessage, List<AssistantSessionTools.ToolCallTrace> traces) {
        if (traces == null || traces.isEmpty()) {
            return;
        }
        for (AssistantSessionTools.ToolCallTrace trace : traces) {
            AssistantToolCall call = new AssistantToolCall();
            call.setMessage(assistantMessage);
            call.setToolName(trace.toolName());
            call.setArgumentsJson(trace.argumentsJson());
            call.setResultSummary(trace.resultSummary());
            call.setSuccess(trace.success());
            call.setDurationMs(trace.durationMs());
            toolCallRepository.save(call);
        }
    }

    private static String deriveTitle(String message) {
        if (message == null || message.isBlank()) {
            return "Nouvelle conversation";
        }
        String compact = message.replaceAll("\\s+", " ").trim();
        return compact.length() <= 80 ? compact : compact.substring(0, 77) + "...";
    }
}
