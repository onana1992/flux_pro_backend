package com.nanotech.flux_pro_backend.service;

import com.nanotech.flux_pro_backend.common.DashboardException;
import com.nanotech.flux_pro_backend.dto.response.DelayByTypeResponse;
import com.nanotech.flux_pro_backend.dto.response.OrganizationRankingResponse;
import com.nanotech.flux_pro_backend.dto.response.OverdueFileResponse;
import com.nanotech.flux_pro_backend.dto.response.WorkloadEntryResponse;
import com.nanotech.flux_pro_backend.security.SecurityUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * Export CSV des widgets DSH (DSH-08, §11.2). Réutilise strictement les mêmes calculs que
 * l'affichage ({@link DashboardService}) : aucune logique métier n'est dupliquée entre export et
 * écran. L'export PDF (Should) n'est pas encore implémenté (cf. SPEC-DSH.md §9.2).
 */
@Service
@RequiredArgsConstructor
public class DashboardExportService {

    public static final String DATASET_OVERDUE_FILES = "overdue-files";
    public static final String DATASET_WORKLOAD = "workload";
    public static final String DATASET_COMPLIANCE_RANKING = "compliance-ranking";
    public static final String DATASET_DELAY_BY_TYPE = "delay-by-type";

    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ISO_INSTANT;

    private final DashboardService dashboardService;

    public byte[] exportCsv(
            String dataset, SecurityUser actor, UUID organizationId, int windowDays, String groupByTypeCode) {
        StringBuilder sb = new StringBuilder("\uFEFF");
        switch (dataset) {
            case DATASET_OVERDUE_FILES -> writeOverdueFiles(sb, actor, organizationId);
            case DATASET_WORKLOAD -> writeWorkload(sb, actor, organizationId);
            case DATASET_COMPLIANCE_RANKING -> writeComplianceRanking(sb, actor, groupByTypeCode, windowDays);
            case DATASET_DELAY_BY_TYPE -> writeDelayByType(sb, actor, organizationId, windowDays);
            default -> throw DashboardException.badRequest(
                    "DASHBOARD_DATASET_INVALID", "Jeu de données inconnu : " + dataset, dataset);
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private void writeOverdueFiles(StringBuilder sb, SecurityUser actor, UUID organizationId) {
        sb.append("reference;objet;type;organisation;maillon;responsable;echeance;jours_de_retard\n");
        List<OverdueFileResponse> rows = dashboardService.overdueFiles(actor, organizationId, 1000);
        for (OverdueFileResponse r : rows) {
            sb.append(csv(r.referenceNumber())).append(';')
                    .append(csv(r.subject())).append(';')
                    .append(csv(r.fileTypeCode())).append(';')
                    .append(csv(r.organizationCode())).append(';')
                    .append(csv(r.stepLabel())).append(';')
                    .append(csv(r.responsibleUserName())).append(';')
                    .append(csv(r.dueAt() != null ? TIMESTAMP_FORMAT.format(r.dueAt()) : "")).append(';')
                    .append(r.daysOverdue()).append('\n');
        }
    }

    private void writeWorkload(StringBuilder sb, SecurityUser actor, UUID organizationId) {
        sb.append("agent;organisation;dossiers_actifs;dossiers_en_retard\n");
        List<WorkloadEntryResponse> rows = dashboardService.workload(actor, organizationId);
        for (WorkloadEntryResponse r : rows) {
            sb.append(csv(r.firstName() + " " + r.lastName())).append(';')
                    .append(csv(r.organizationCode())).append(';')
                    .append(r.activeCount()).append(';')
                    .append(r.overdueCount()).append('\n');
        }
    }

    private void writeComplianceRanking(StringBuilder sb, SecurityUser actor, String groupByTypeCode, int windowDays) {
        sb.append("organisation;code;dossiers_clotures;dossiers_conformes;taux_de_respect_pct\n");
        List<OrganizationRankingResponse> rows = dashboardService.complianceRanking(actor, groupByTypeCode, windowDays);
        for (OrganizationRankingResponse r : rows) {
            sb.append(csv(r.organizationName())).append(';')
                    .append(csv(r.organizationCode())).append(';')
                    .append(r.closedCount()).append(';')
                    .append(r.compliantCount()).append(';')
                    .append(r.complianceRate()).append('\n');
        }
    }

    private void writeDelayByType(StringBuilder sb, SecurityUser actor, UUID organizationId, int windowDays) {
        sb.append("type_de_dossier;dossiers_clotures;delai_moyen_jours;delai_cible_jours\n");
        List<DelayByTypeResponse> rows = dashboardService.delayByType(actor, organizationId, windowDays);
        for (DelayByTypeResponse r : rows) {
            sb.append(csv(r.fileTypeLabel())).append(';')
                    .append(r.closedCount()).append(';')
                    .append(r.averageDelayDays()).append(';')
                    .append(r.targetDelayDays() != null ? r.targetDelayDays() : "").append('\n');
        }
    }

    private String csv(String value) {
        if (value == null) {
            return "";
        }
        String escaped = value.replace("\"", "\"\"");
        if (escaped.contains(";") || escaped.contains("\"") || escaped.contains("\n")) {
            return "\"" + escaped + "\"";
        }
        return escaped;
    }
}
