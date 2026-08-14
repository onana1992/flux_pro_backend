package com.nanotech.flux_pro_backend.service.assistant;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * KB aide locale (help-kb.md) — recherche lexicale légère (pas d'embeddings).
 */
@Component
@Slf4j
public class AssistantHelpKnowledgeBase {

    private static final Pattern SECTION_SPLIT = Pattern.compile("(?m)^##\\s+");

    private final List<HelpSection> sections = new ArrayList<>();

    @PostConstruct
    void load() {
        sections.clear();
        try {
            Resource resource = new ClassPathResource("assistant/help-kb.md");
            if (!resource.exists()) {
                log.warn("assistant/help-kb.md introuvable sur le classpath");
                return;
            }
            String raw = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
            String[] parts = SECTION_SPLIT.split(raw);
            for (String part : parts) {
                String trimmed = part.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("# ")) {
                    continue;
                }
                int nl = trimmed.indexOf('\n');
                String id = (nl < 0 ? trimmed : trimmed.substring(0, nl)).trim();
                String body = nl < 0 ? "" : trimmed.substring(nl + 1).trim();
                if (!id.isBlank()) {
                    sections.add(new HelpSection(id, body, normalize(id + "\n" + body)));
                }
            }
            log.info("Assistant help-kb : {} sections chargées", sections.size());
        } catch (Exception e) {
            log.warn("Chargement help-kb échoué : {}", e.getMessage());
        }
    }

    public Map<String, Object> lookup(String query, int limit) {
        int lim = Math.max(1, Math.min(5, limit));
        String q = normalize(query == null ? "" : query);
        List<Map<String, Object>> hits = new ArrayList<>();
        if (q.isBlank() || sections.isEmpty()) {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("query", query);
            payload.put("hits", hits);
            payload.put("sectionIds", sections.stream().map(HelpSection::id).toList());
            return payload;
        }
        String[] tokens = q.split("\\s+");
        record Scored(HelpSection section, int score) {}
        List<Scored> scored = new ArrayList<>();
        for (HelpSection section : sections) {
            int score = 0;
            if (section.normalized().contains(q)) {
                score += 10;
            }
            for (String token : tokens) {
                if (token.length() < 3) {
                    continue;
                }
                if (section.id().toLowerCase(Locale.ROOT).contains(token)) {
                    score += 5;
                }
                if (section.normalized().contains(token)) {
                    score += 2;
                }
            }
            if (score > 0) {
                scored.add(new Scored(section, score));
            }
        }
        scored.sort(Comparator.comparingInt(Scored::score).reversed());
        for (Scored s : scored.stream().limit(lim).toList()) {
            Map<String, Object> hit = new LinkedHashMap<>();
            hit.put("sectionId", s.section().id());
            hit.put("score", s.score());
            hit.put("content", s.section().body());
            hits.add(hit);
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("query", query);
        payload.put("hits", hits);
        if (hits.isEmpty()) {
            payload.put("hint", "Aucune section exacte ; reformulez (transmettre, créer, dashboard, template, férié…).");
        }
        return payload;
    }

    private static String normalize(String raw) {
        String n = Normalizer.normalize(raw, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT);
        return n.replaceAll("[^a-z0-9_/\\-\\s]+", " ").replaceAll("\\s+", " ").trim();
    }

    private record HelpSection(String id, String body, String normalized) {
    }
}
