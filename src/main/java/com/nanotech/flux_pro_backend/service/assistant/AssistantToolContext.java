package com.nanotech.flux_pro_backend.service.assistant;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.nanotech.flux_pro_backend.dto.response.AssistantCitationResponse;
import com.nanotech.flux_pro_backend.security.SecurityUser;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * État partagé d'une requête assistant (citations, traces, quota tool-calls).
 */
public final class AssistantToolContext {

    static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private final SecurityUser actor;
    private final int maxToolCalls;
    private final List<String> toolsUsed = new ArrayList<>();
    private final List<AssistantCitationResponse> citations = new ArrayList<>();
    private final List<AssistantSessionTools.ToolCallTrace> toolTraces = new ArrayList<>();

    public AssistantToolContext(SecurityUser actor, int maxToolCalls) {
        this.actor = actor;
        this.maxToolCalls = maxToolCalls <= 0 ? 5 : maxToolCalls;
    }

    public SecurityUser actor() {
        return actor;
    }

    public List<String> getToolsUsed() {
        return List.copyOf(toolsUsed);
    }

    public List<AssistantCitationResponse> getCitations() {
        return List.copyOf(citations);
    }

    public List<AssistantSessionTools.ToolCallTrace> getToolTraces() {
        return List.copyOf(toolTraces);
    }

    public boolean hasPerm(String permission) {
        return actor.getPermissionNames() != null && actor.getPermissionNames().contains(permission);
    }

    public void addFileCitation(UUID id, String label) {
        if (id == null) {
            return;
        }
        String ref = label != null ? label : id.toString();
        boolean exists = citations.stream()
                .anyMatch(c -> "file".equals(c.type()) && id.equals(c.id()));
        if (!exists) {
            citations.add(new AssistantCitationResponse("file", id, ref, "/files/" + id));
        }
    }

    public void addCitation(String type, UUID id, String label, String href) {
        boolean exists = citations.stream()
                .anyMatch(c -> type.equals(c.type())
                        && ((id == null && c.id() == null) || (id != null && id.equals(c.id())))
                        && href.equals(c.href()));
        if (!exists) {
            citations.add(new AssistantCitationResponse(type, id, label, href));
        }
    }

    public String runTool(String name, Map<String, ?> args, ToolBody body) {
        long start = System.currentTimeMillis();
        if (toolsUsed.size() >= maxToolCalls) {
            String limited = errorJson(
                    "TOOL_CALL_LIMIT",
                    "Limite de " + maxToolCalls
                            + " appels d'outils atteinte pour ce tour. Répondez avec les données déjà obtenues.");
            toolsUsed.add(name);
            toolTraces.add(new AssistantSessionTools.ToolCallTrace(
                    name, toJsonQuiet(args), summarize(limited), false,
                    (int) (System.currentTimeMillis() - start)));
            return limited;
        }
        toolsUsed.add(name);
        try {
            String result = body.execute();
            toolTraces.add(new AssistantSessionTools.ToolCallTrace(
                    name, toJsonQuiet(args), summarize(result), true,
                    (int) (System.currentTimeMillis() - start)));
            return result;
        } catch (Exception ex) {
            String err = errorJson(ex.getClass().getSimpleName(), safeMessage(ex));
            toolTraces.add(new AssistantSessionTools.ToolCallTrace(
                    name, toJsonQuiet(args), summarize(err), false,
                    (int) (System.currentTimeMillis() - start)));
            return err;
        }
    }

    public String toJson(Object value) {
        try {
            return MAPPER.writeValueAsString(value);
        } catch (Exception e) {
            return "{\"error\":\"SERIALIZATION_FAILED\"}";
        }
    }

    public String errorJson(String code, String message) {
        Map<String, Object> err = new LinkedHashMap<>();
        err.put("error", code);
        err.put("message", message);
        return toJson(err);
    }

    private String toJsonQuiet(Object value) {
        try {
            return MAPPER.writeValueAsString(value);
        } catch (Exception e) {
            return null;
        }
    }

    private static String summarize(String result) {
        if (result == null) {
            return null;
        }
        String compact = result.replaceAll("\\s+", " ").trim();
        return compact.length() <= 500 ? compact : compact.substring(0, 497) + "...";
    }

    private static String safeMessage(Exception ex) {
        String msg = ex.getMessage();
        if (msg == null || msg.isBlank()) {
            return ex.getClass().getSimpleName();
        }
        return msg.length() <= 300 ? msg : msg.substring(0, 297) + "...";
    }

    @FunctionalInterface
    public interface ToolBody {
        String execute() throws Exception;
    }
}
