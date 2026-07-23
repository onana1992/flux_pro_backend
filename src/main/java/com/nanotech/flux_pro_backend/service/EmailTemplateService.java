package com.nanotech.flux_pro_backend.service;

import com.nanotech.flux_pro_backend.email.EmailMessageModel;
import com.nanotech.flux_pro_backend.email.EmailTonePalette;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.util.Locale;
import java.util.Set;

/**
 * Résolution et rendu des gabarits email via {@code AlertType.emailTemplateCode}
 * (SPEC-ALR §9.2). Repli sur {@code alert-generic} si code absent ou fichier manquant.
 */
@Service
@Slf4j
public class EmailTemplateService {

    public static final String GENERIC_TEMPLATE = "alert-generic";

    private static final Set<String> KNOWN_TONES = Set.of(
            "navy", "teal", "amber", "crimson", "slate", "forest");

    private TemplateEngine templateEngine;

    @PostConstruct
    void init() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/email/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");
        resolver.setCacheable(true);
        resolver.setCheckExistence(true);

        TemplateEngine engine = new TemplateEngine();
        engine.setTemplateResolver(resolver);
        this.templateEngine = engine;
    }

    public String resolveTemplateCode(String emailTemplateCode) {
        if (emailTemplateCode == null || emailTemplateCode.isBlank()) {
            return GENERIC_TEMPLATE;
        }
        String code = emailTemplateCode.trim();
        if (!templateExists(code)) {
            log.warn("ALR: gabarit email '{}' introuvable — repli sur {}", code, GENERIC_TEMPLATE);
            return GENERIC_TEMPLATE;
        }
        return code;
    }

    public boolean templateExists(String code) {
        if (code == null || code.isBlank()) {
            return false;
        }
        return new ClassPathResource("templates/email/" + code.trim() + ".html").exists();
    }

    public String render(String emailTemplateCode, EmailMessageModel model) {
        String code = resolveTemplateCode(emailTemplateCode);
        String tone = normalizeTone(model.tone());
        Context ctx = new Context(Locale.FRENCH);
        ctx.setVariable("m", model);
        ctx.setVariable("tone", tone);
        ctx.setVariable("palette", EmailTonePalette.forTone(tone));
        return templateEngine.process(code, ctx);
    }

    private static String normalizeTone(String tone) {
        if (tone == null || tone.isBlank()) {
            return "navy";
        }
        String t = tone.trim().toLowerCase(Locale.ROOT);
        return KNOWN_TONES.contains(t) ? t : "navy";
    }
}
