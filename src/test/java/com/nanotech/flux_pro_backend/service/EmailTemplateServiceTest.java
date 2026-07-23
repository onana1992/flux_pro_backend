package com.nanotech.flux_pro_backend.service;

import com.nanotech.flux_pro_backend.email.EmailDigestItem;
import com.nanotech.flux_pro_backend.email.EmailMessageModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EmailTemplateServiceTest {

    private EmailTemplateService service;

    @BeforeEach
    void setUp() {
        service = new EmailTemplateService();
        service.init();
    }

    @Test
    void resolveTemplateCode_null_fallsBackToGeneric() {
        assertThat(service.resolveTemplateCode(null)).isEqualTo(EmailTemplateService.GENERIC_TEMPLATE);
        assertThat(service.resolveTemplateCode("  ")).isEqualTo(EmailTemplateService.GENERIC_TEMPLATE);
    }

    @Test
    void resolveTemplateCode_unknown_fallsBackToGeneric() {
        assertThat(service.resolveTemplateCode("does-not-exist")).isEqualTo(EmailTemplateService.GENERIC_TEMPLATE);
    }

    @Test
    void resolveTemplateCode_knownSeedCodes() {
        assertThat(service.resolveTemplateCode("alert-reminder")).isEqualTo("alert-reminder");
        assertThat(service.resolveTemplateCode("alert-overdue")).isEqualTo("alert-overdue");
        assertThat(service.resolveTemplateCode("alert-escalation")).isEqualTo("alert-escalation");
        assertThat(service.resolveTemplateCode("passage-arrival")).isEqualTo("passage-arrival");
        assertThat(service.resolveTemplateCode("passage-cc")).isEqualTo("passage-cc");
        assertThat(service.resolveTemplateCode("alert-daily-digest")).isEqualTo("alert-daily-digest");
    }

    @Test
    void render_reminder_containsFileReferenceAndLabel() {
        EmailMessageModel model = EmailMessageModel.builder()
                .productName("FluxPro")
                .tenantBadge("MINTP")
                .alertLabel("Rappel avant échéance")
                .intro("Un rappel vous est adressé.")
                .tone("teal")
                .recipientFirstName("Amina")
                .fileReference("MINTP-2026-00042")
                .fileSubject("Demande d'agrément")
                .stepLabel("Instruction")
                .dueAtFormatted("30/07/2026 17:00")
                .fileUrl("http://localhost:3000/files/abc")
                .build();

        String html = service.render("alert-reminder", model);

        assertThat(html).contains("Rappel avant échéance");
        assertThat(html).contains("MINTP-2026-00042");
        assertThat(html).contains("Demande d&#39;agrément");
        assertThat(html).contains("Instruction");
        assertThat(html).contains("Bonjour");
        assertThat(html).contains("Amina");
        assertThat(html).contains("http://localhost:3000/files/abc");
    }

    @Test
    void render_digest_containsTableRows() {
        EmailMessageModel model = EmailMessageModel.builder()
                .productName("FluxPro")
                .alertLabel("Récapitulatif quotidien des retards")
                .intro("Voici les dossiers en retard.")
                .tone("navy")
                .recipientFirstName("Paul")
                .fileUrl("http://localhost:3000")
                .ctaLabel("Accéder à FluxPro")
                .digestItems(List.of(
                        new EmailDigestItem(
                                "MINTP-1", "Objet A", "Visa", "20/07/2026", 3, "Alice Martin",
                                "http://localhost:3000/files/1"),
                        new EmailDigestItem(
                                "MINTP-2", "Objet B", "Signature", "18/07/2026", 5, "Bob Ngu",
                                "http://localhost:3000/files/2")))
                .build();

        String html = service.render("alert-daily-digest", model);

        assertThat(html).contains("MINTP-1");
        assertThat(html).contains("MINTP-2");
        assertThat(html).contains("Objet A");
        assertThat(html).contains("Alice Martin");
        assertThat(html).contains(">3<");
    }
}
