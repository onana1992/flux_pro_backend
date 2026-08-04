package com.nanotech.flux_pro_backend.service.assistant;

import com.nanotech.flux_pro_backend.common.AppException;
import com.nanotech.flux_pro_backend.config.AssistantProperties;
import com.nanotech.flux_pro_backend.dto.request.AssistantChatRequest;
import com.nanotech.flux_pro_backend.dto.response.AssistantChatResponse;
import com.nanotech.flux_pro_backend.dto.response.AssistantCitationResponse;
import com.nanotech.flux_pro_backend.entity.AssistantConversation;
import com.nanotech.flux_pro_backend.entity.AssistantMessage;
import com.nanotech.flux_pro_backend.enumeration.AssistantMessageRole;
import com.nanotech.flux_pro_backend.security.AccessControlService;
import com.nanotech.flux_pro_backend.security.OrganizationScopeService;
import com.nanotech.flux_pro_backend.security.SecurityUser;
import com.nanotech.flux_pro_backend.service.AlertRuleService;
import com.nanotech.flux_pro_backend.service.AlertTypeService;
import com.nanotech.flux_pro_backend.service.BusinessCalendarDayService;
import com.nanotech.flux_pro_backend.service.ChainTemplateService;
import com.nanotech.flux_pro_backend.service.DashboardService;
import com.nanotech.flux_pro_backend.service.FileAttachmentService;
import com.nanotech.flux_pro_backend.service.FileService;
import com.nanotech.flux_pro_backend.service.FileTypeService;
import com.nanotech.flux_pro_backend.service.NotificationService;
import com.nanotech.flux_pro_backend.service.OrganizationService;
import com.nanotech.flux_pro_backend.service.PassageService;
import com.nanotech.flux_pro_backend.service.PreconfiguredDossierService;
import com.nanotech.flux_pro_backend.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class AssistantOrchestrator {

    private static final Pattern WRITE_INTENT = Pattern.compile(
            "(?i)\\b(transmet|transmettre|cl[oô]tur|annul|supprim|cr[eé]e|modifi|r[eé]assigne|suspend|"
                    + "transmit|close|cancel|delete|create|update|reassign)\\b");

    private static final Pattern HOW_TO_INTENT = Pattern.compile(
            "(?i)^(comment|how\\b|où\\s+(voir|trouver|accéder)|ou\\s+(voir|trouver)|pourquoi\\s+je\\s+n)");

    private static final String FALLBACK_SYSTEM_PROMPT = """
            Tu es l'assistant métier FluxPro (lecture seule). Grounding obligatoire via outils.
            Aucune action métier. Français administratif clair. Cite /files/{id} et /dashboard.
            Pour « comment faire », utilise lookup_help.
            """;

    private final AssistantProperties properties;
    private final ObjectProvider<ChatClient> chatClientProvider;
    private final AssistantConversationService conversationService;
    private final AssistantRateLimiter rateLimiter;
    private final AssistantHelpKnowledgeBase helpKnowledgeBase;
    private final UserService userService;
    private final FileService fileService;
    private final PassageService passageService;
    private final FileAttachmentService fileAttachmentService;
    private final NotificationService notificationService;
    private final DashboardService dashboardService;
    private final OrganizationService organizationService;
    private final OrganizationScopeService organizationScopeService;
    private final AccessControlService accessControlService;
    private final FileTypeService fileTypeService;
    private final ChainTemplateService chainTemplateService;
    private final AlertTypeService alertTypeService;
    private final AlertRuleService alertRuleService;
    private final BusinessCalendarDayService businessCalendarDayService;
    private final PreconfiguredDossierService preconfiguredDossierService;

    private volatile String cachedSystemPrompt;

    public AssistantChatResponse chat(AssistantChatRequest request, SecurityUser actor) {
        if (!properties.enabled()) {
            throw AppException.notFound("ASSISTANT_DISABLED", "Assistant IA désactivé");
        }
        ChatClient chatClient = chatClientProvider.getIfAvailable();
        if (chatClient == null) {
            throw AppException.internal(
                    "ASSISTANT_LLM_NOT_CONFIGURED",
                    "Assistant activé mais modèle LLM non configuré");
        }

        rateLimiter.checkOrThrow(actor.getId());

        String userText = request.message().trim();
        boolean refused = looksLikeWriteIntent(userText);
        boolean howTo = looksLikeHowTo(userText);

        AssistantConversation conversation = conversationService.getOrCreate(
                request.conversationId(), actor, userText);
        conversationService.saveMessage(conversation, AssistantMessageRole.USER, userText, false);

        List<AssistantMessage> history = conversationService.loadHistory(
                conversation.getId(), properties.maxHistoryMessages());

        AssistantToolContext toolCtx = new AssistantToolContext(actor, properties.maxToolCalls());
        AssistantSessionTools opsTools = AssistantSessionTools.forUser(
                toolCtx,
                userService,
                fileService,
                passageService,
                fileAttachmentService,
                notificationService,
                dashboardService);
        AssistantCatalogTools catalogTools = new AssistantCatalogTools(
                toolCtx,
                organizationService,
                organizationScopeService,
                accessControlService,
                userService,
                fileTypeService,
                chainTemplateService,
                alertTypeService,
                alertRuleService,
                businessCalendarDayService,
                preconfiguredDossierService,
                helpKnowledgeBase);

        String answer;
        if (refused) {
            MapHelpRefusal help = loadGuideFallback(userText);
            answer = help.answer();
            if (!help.citations().isEmpty()) {
                help.citations().forEach(c ->
                        toolCtx.addCitation(c.type(), c.id(), c.label(), c.href()));
            }
        } else {
            answer = callLlm(chatClient, history, userText, opsTools, catalogTools, howTo);
        }

        AssistantMessage assistantMessage = conversationService.saveMessage(
                conversation, AssistantMessageRole.ASSISTANT, answer, refused);
        conversationService.saveToolCalls(assistantMessage, toolCtx.getToolTraces());

        List<AssistantCitationResponse> citations = new ArrayList<>(toolCtx.getCitations());
        return new AssistantChatResponse(
                conversation.getId(),
                assistantMessage.getId(),
                answer,
                citations,
                toolCtx.getToolsUsed(),
                refused);
    }

    private String callLlm(
            ChatClient chatClient,
            List<AssistantMessage> history,
            String userText,
            AssistantSessionTools opsTools,
            AssistantCatalogTools catalogTools,
            boolean howTo) {
        String historyBlock = buildHistoryBlock(history, userText);
        String guideHint = howTo
                ? "\n\nMode guide UI : privilégie lookup_help, donne étapes + liens, aucune mutation.\n"
                : "\n";
        String system = loadSystemPrompt() + guideHint + "\nHistorique récent :\n" + historyBlock;
        Callable<String> task = () -> chatClient.prompt()
                .system(system)
                .user(userText)
                .tools(opsTools, catalogTools)
                .call()
                .content();

        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Future<String> future = executor.submit(task);
            String content = future.get(properties.timeoutSeconds(), TimeUnit.SECONDS);
            if (content == null || content.isBlank()) {
                return "Je n'ai pas pu formuler de réponse. Réessayez ou reformulez votre question.";
            }
            return content.trim();
        } catch (TimeoutException e) {
            log.warn("Assistant LLM timeout after {}s", properties.timeoutSeconds());
            throw AppException.internal(
                    "ASSISTANT_TIMEOUT",
                    "Délai dépassé en attendant la réponse de l'assistant. Réessayez.");
        } catch (ExecutionException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            log.error("Assistant LLM call failed: {}", cause.getMessage());
            if (cause instanceof AppException appEx) {
                throw appEx;
            }
            throw AppException.internal(
                    "ASSISTANT_LLM_ERROR",
                    "Erreur lors de l'appel au modèle IA : " + safe(cause.getMessage()));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw AppException.internal("ASSISTANT_INTERRUPTED", "Appel assistant interrompu");
        } finally {
            executor.shutdownNow();
        }
    }

    private MapHelpRefusal loadGuideFallback(String userText) {
        var lookup = helpKnowledgeBase.lookup(userText, 1);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> hits = (List<Map<String, Object>>) lookup.get("hits");
        StringBuilder sb = new StringBuilder();
        sb.append("Je suis un assistant en **lecture seule** : je ne peux pas exécuter cette action.\n\n");
        List<AssistantCitationResponse> cites = new ArrayList<>();
        if (hits != null && !hits.isEmpty()) {
            Object content = hits.get(0).get("content");
            if (content != null) {
                sb.append(content).append("\n\n");
            }
        } else {
            sb.append("Ouvrez le dossier dans FluxPro (`/files/…`) pour l'action, ")
                    .append("ou demandez-moi « comment transmettre / clôturer / créer » pour le guide pas à pas.\n");
        }
        cites.add(new AssistantCitationResponse("help", null, "Dossiers", "/files"));
        cites.add(new AssistantCitationResponse("dashboard", null, "Tableau de bord", "/dashboard"));
        return new MapHelpRefusal(sb.toString().trim(), cites);
    }

    private String loadSystemPrompt() {
        if (cachedSystemPrompt != null) {
            return cachedSystemPrompt;
        }
        synchronized (this) {
            if (cachedSystemPrompt != null) {
                return cachedSystemPrompt;
            }
            try {
                ClassPathResource resource = new ClassPathResource("assistant/system-prompt.md");
                if (resource.exists()) {
                    cachedSystemPrompt = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
                } else {
                    cachedSystemPrompt = FALLBACK_SYSTEM_PROMPT;
                }
            } catch (Exception e) {
                log.warn("Impossible de charger system-prompt.md : {}", e.getMessage());
                cachedSystemPrompt = FALLBACK_SYSTEM_PROMPT;
            }
            return cachedSystemPrompt;
        }
    }

    private static String buildHistoryBlock(List<AssistantMessage> history, String currentUserText) {
        StringBuilder sb = new StringBuilder();
        boolean skippedCurrent = false;
        for (int i = history.size() - 1; i >= 0; i--) {
            AssistantMessage msg = history.get(i);
            if (!skippedCurrent
                    && msg.getRole() == AssistantMessageRole.USER
                    && currentUserText.equals(msg.getContent())) {
                skippedCurrent = true;
                continue;
            }
            sb.insert(0, msg.getRole().name() + ": " + msg.getContent() + "\n");
        }
        if (sb.isEmpty()) {
            return "(aucun)";
        }
        return sb.toString();
    }

    private static boolean looksLikeWriteIntent(String text) {
        String normalized = text.trim().toLowerCase(Locale.ROOT);
        if (normalized.startsWith("comment")
                || normalized.startsWith("how ")
                || normalized.startsWith("où")
                || normalized.startsWith("ou ")
                || normalized.startsWith("qui")
                || normalized.startsWith("quel")
                || normalized.startsWith("combien")
                || normalized.startsWith("pourquoi")) {
            return false;
        }
        return WRITE_INTENT.matcher(text).find();
    }

    private static boolean looksLikeHowTo(String text) {
        return HOW_TO_INTENT.matcher(text.trim()).find();
    }

    private static String safe(String message) {
        if (message == null || message.isBlank()) {
            return "erreur inconnue";
        }
        return message.length() <= 200 ? message : message.substring(0, 197) + "...";
    }

    private record MapHelpRefusal(String answer, List<AssistantCitationResponse> citations) {
    }
}
