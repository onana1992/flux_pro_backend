package com.nanotech.flux_pro_backend.email;

import java.util.List;

/**
 * Modèle commun pour le rendu des gabarits email (ALR-05 / ALR-08).
 */
public record EmailMessageModel(
        String productName,
        String tenantBadge,
        String alertLabel,
        String alertDescription,
        String intro,
        String tone,
        String recipientFirstName,
        String fileReference,
        String fileSubject,
        String stepLabel,
        String dueAtFormatted,
        Integer overdueWorkingDays,
        Integer escalationLevel,
        String responsibleName,
        String fileUrl,
        String ctaLabel,
        List<EmailDigestItem> digestItems,
        String redirectNotice
) {
    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String productName;
        private String tenantBadge;
        private String alertLabel;
        private String alertDescription;
        private String intro;
        private String tone = "navy";
        private String recipientFirstName;
        private String fileReference;
        private String fileSubject;
        private String stepLabel;
        private String dueAtFormatted;
        private Integer overdueWorkingDays;
        private Integer escalationLevel;
        private String responsibleName;
        private String fileUrl;
        private String ctaLabel = "Ouvrir le dossier";
        private List<EmailDigestItem> digestItems = List.of();
        private String redirectNotice;

        public Builder productName(String v) { this.productName = v; return this; }
        public Builder tenantBadge(String v) { this.tenantBadge = v; return this; }
        public Builder alertLabel(String v) { this.alertLabel = v; return this; }
        public Builder alertDescription(String v) { this.alertDescription = v; return this; }
        public Builder intro(String v) { this.intro = v; return this; }
        public Builder tone(String v) { this.tone = v; return this; }
        public Builder recipientFirstName(String v) { this.recipientFirstName = v; return this; }
        public Builder fileReference(String v) { this.fileReference = v; return this; }
        public Builder fileSubject(String v) { this.fileSubject = v; return this; }
        public Builder stepLabel(String v) { this.stepLabel = v; return this; }
        public Builder dueAtFormatted(String v) { this.dueAtFormatted = v; return this; }
        public Builder overdueWorkingDays(Integer v) { this.overdueWorkingDays = v; return this; }
        public Builder escalationLevel(Integer v) { this.escalationLevel = v; return this; }
        public Builder responsibleName(String v) { this.responsibleName = v; return this; }
        public Builder fileUrl(String v) { this.fileUrl = v; return this; }
        public Builder ctaLabel(String v) { this.ctaLabel = v; return this; }
        public Builder digestItems(List<EmailDigestItem> v) { this.digestItems = v != null ? v : List.of(); return this; }
        public Builder redirectNotice(String v) { this.redirectNotice = v; return this; }

        public EmailMessageModel build() {
            return new EmailMessageModel(
                    productName,
                    tenantBadge,
                    alertLabel,
                    alertDescription,
                    intro,
                    tone,
                    recipientFirstName,
                    fileReference,
                    fileSubject,
                    stepLabel,
                    dueAtFormatted,
                    overdueWorkingDays,
                    escalationLevel,
                    responsibleName,
                    fileUrl,
                    ctaLabel,
                    digestItems,
                    redirectNotice);
        }
    }
}
