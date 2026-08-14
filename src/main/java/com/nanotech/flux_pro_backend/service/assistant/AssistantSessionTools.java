package com.nanotech.flux_pro_backend.service.assistant;

import com.nanotech.flux_pro_backend.dto.response.AlertResponse;
import com.nanotech.flux_pro_backend.dto.response.AssistantCitationResponse;
import com.nanotech.flux_pro_backend.dto.response.CurrentHolderResponse;
import com.nanotech.flux_pro_backend.dto.response.DashboardAnalyticsResponse;
import com.nanotech.flux_pro_backend.dto.response.DashboardSummaryResponse;
import com.nanotech.flux_pro_backend.dto.response.DelayByTypeResponse;
import com.nanotech.flux_pro_backend.dto.response.FileAttachmentResponse;
import com.nanotech.flux_pro_backend.dto.response.FileDetailResponse;
import com.nanotech.flux_pro_backend.dto.response.FilePassageCircuitResponse;
import com.nanotech.flux_pro_backend.dto.response.FileSummaryResponse;
import com.nanotech.flux_pro_backend.dto.response.MyActivityResponse;
import com.nanotech.flux_pro_backend.dto.response.OrganizationRankingResponse;
import com.nanotech.flux_pro_backend.dto.response.OverdueFileResponse;
import com.nanotech.flux_pro_backend.dto.response.PassageResponse;
import com.nanotech.flux_pro_backend.dto.response.UserProfileResponse;
import com.nanotech.flux_pro_backend.dto.response.WorkloadEntryResponse;
import com.nanotech.flux_pro_backend.entity.Alert;
import com.nanotech.flux_pro_backend.entity.FileEntity;
import com.nanotech.flux_pro_backend.enumeration.FilePriority;
import com.nanotech.flux_pro_backend.enumeration.FileStatus;
import com.nanotech.flux_pro_backend.mapper.AlertMapper;
import com.nanotech.flux_pro_backend.security.RbacPermissions;
import com.nanotech.flux_pro_backend.security.SecurityUser;
import com.nanotech.flux_pro_backend.service.DashboardService;
import com.nanotech.flux_pro_backend.service.FileAttachmentService;
import com.nanotech.flux_pro_backend.service.FileService;
import com.nanotech.flux_pro_backend.service.NotificationService;
import com.nanotech.flux_pro_backend.service.PassageService;
import com.nanotech.flux_pro_backend.service.UserService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Tools lecture seule Pack P0/P1 — dossiers, passation, notifications, dashboard.
 */
public class AssistantSessionTools {

    private static final int MAX_LIST = 10;

    private final AssistantToolContext ctx;
    private final SecurityUser actor;
    private final UserService userService;
    private final FileService fileService;
    private final PassageService passageService;
    private final FileAttachmentService fileAttachmentService;
    private final NotificationService notificationService;
    private final DashboardService dashboardService;

    public AssistantSessionTools(
            AssistantToolContext ctx,
            UserService userService,
            FileService fileService,
            PassageService passageService,
            FileAttachmentService fileAttachmentService,
            NotificationService notificationService,
            DashboardService dashboardService) {
        this.ctx = ctx;
        this.actor = ctx.actor();
        this.userService = userService;
        this.fileService = fileService;
        this.passageService = passageService;
        this.fileAttachmentService = fileAttachmentService;
        this.notificationService = notificationService;
        this.dashboardService = dashboardService;
    }

    public static AssistantSessionTools forUser(
            SecurityUser actor,
            UserService userService,
            FileService fileService,
            PassageService passageService,
            FileAttachmentService fileAttachmentService,
            NotificationService notificationService,
            DashboardService dashboardService,
            int maxToolCalls) {
        return forUser(
                new AssistantToolContext(actor, maxToolCalls),
                userService, fileService, passageService,
                fileAttachmentService, notificationService, dashboardService);
    }

    public static AssistantSessionTools forUser(
            AssistantToolContext ctx,
            UserService userService,
            FileService fileService,
            PassageService passageService,
            FileAttachmentService fileAttachmentService,
            NotificationService notificationService,
            DashboardService dashboardService) {
        return new AssistantSessionTools(
                ctx, userService, fileService, passageService,
                fileAttachmentService, notificationService, dashboardService);
    }

    public AssistantToolContext context() {
        return ctx;
    }

    public List<String> getToolsUsed() {
        return ctx.getToolsUsed();
    }

    public List<AssistantCitationResponse> getCitations() {
        return ctx.getCitations();
    }

    public List<ToolCallTrace> getToolTraces() {
        return ctx.getToolTraces();
    }

    // --- Profil ---

    @Tool(name = "get_current_user", description = """
            Retourne le profil de l'utilisateur authentifié (nom, email, rôle, organisation).
            Questions : « Qui suis-je ? », « Quel est mon rôle ? ».
            """)
    public String getCurrentUser() {
        return runTool("get_current_user", Map.of(), () -> {
            UserProfileResponse profile = userService.getMeProfile(actor);
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("id", profile.id());
            payload.put("email", profile.email());
            payload.put("firstName", profile.firstName());
            payload.put("lastName", profile.lastName());
            payload.put("role", profile.role() != null ? profile.role().name() : null);
            payload.put("roles", profile.roles());
            if (profile.organization() != null) {
                payload.put("organizationId", profile.organization().id());
                payload.put("organizationCode", profile.organization().code());
                payload.put("organizationName", profile.organization().name());
            }
            return toJson(payload);
        });
    }

    // --- Dossiers ---

    @Tool(name = "search_files", description = """
            Recherche des dossiers accessibles (max 10 résultats).
            Filtres optionnels : search (texte), status (DRAFT|IN_PROGRESS|CLOSED|CANCELLED|ARCHIVED),
            priority (LOW|NORMAL|HIGH|URGENT), fileTypeCode, organizationId (UUID),
            receivedFrom / receivedTo (YYYY-MM-DD), page (0-based).
            """)
    public String searchFiles(
            @ToolParam(required = false, description = "Texte libre (référence, objet…)") String search,
            @ToolParam(required = false, description = "Statut FileStatus") String status,
            @ToolParam(required = false, description = "Priorité FilePriority") String priority,
            @ToolParam(required = false, description = "Code type de dossier") String fileTypeCode,
            @ToolParam(required = false, description = "UUID organisation") String organizationId,
            @ToolParam(required = false, description = "Date réception depuis YYYY-MM-DD") String receivedFrom,
            @ToolParam(required = false, description = "Date réception jusqu'à YYYY-MM-DD") String receivedTo,
            @ToolParam(required = false, description = "Page 0-based") Integer page) {
        Map<String, Object> args = new LinkedHashMap<>();
        args.put("search", search);
        args.put("status", status);
        args.put("priority", priority);
        args.put("fileTypeCode", fileTypeCode);
        args.put("organizationId", organizationId);
        args.put("receivedFrom", receivedFrom);
        args.put("receivedTo", receivedTo);
        args.put("page", page);
        return runTool("search_files", args, () -> {
            if (!hasPerm(RbacPermissions.FILES_READ)) {
                return errorJson("PERMISSION_DENIED", "Permission FILES:READ requise.");
            }
            int pageIdx = page == null || page < 0 ? 0 : page;
            Page<FileSummaryResponse> result = fileService.findAll(
                    blankToNull(search),
                    parseUuid(organizationId),
                    blankToNull(fileTypeCode),
                    parseEnum(FileStatus.class, status),
                    parseEnum(FilePriority.class, priority),
                    parseDate(receivedFrom),
                    parseDate(receivedTo),
                    null,
                    PageRequest.of(pageIdx, MAX_LIST),
                    actor);
            List<Map<String, Object>> items = new ArrayList<>();
            for (FileSummaryResponse f : result.getContent()) {
                items.add(fileSummaryMap(f));
                ctx.addFileCitation(f.id(), f.referenceNumber());
            }
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("totalElements", result.getTotalElements());
            payload.put("page", result.getNumber());
            payload.put("size", result.getSize());
            payload.put("items", items);
            return toJson(payload);
        });
    }

    @Tool(name = "get_file_by_reference", description = """
            Localise un dossier par numéro de référence (ex. MINTP-DAG-2026-0042).
            Inclut possessionnaire actuel si circuit actif.
            """)
    public String getFileByReference(
            @ToolParam(description = "Numéro de référence du dossier") String reference) {
        return runTool("get_file_by_reference", Map.of("reference", nz(reference)), () -> {
            if (!hasPerm(RbacPermissions.FILES_READ)) {
                return errorJson("PERMISSION_DENIED", "Permission FILES:READ requise.");
            }
            if (reference == null || reference.isBlank()) {
                return errorJson("INVALID_REFERENCE", "Référence dossier manquante.");
            }
            FileDetailResponse file = fileService.findByReference(reference.trim(), actor);
            return toJson(fileDetailWithCircuit(file));
        });
    }

    @Tool(name = "get_file_by_id", description = "Détail d'un dossier par UUID (id).")
    public String getFileById(
            @ToolParam(description = "UUID du dossier") String fileId) {
        return runTool("get_file_by_id", Map.of("fileId", nz(fileId)), () -> {
            if (!hasPerm(RbacPermissions.FILES_READ)) {
                return errorJson("PERMISSION_DENIED", "Permission FILES:READ requise.");
            }
            UUID id = requireUuid(fileId, "fileId");
            if (id == null) {
                return errorJson("INVALID_ID", "fileId UUID invalide.");
            }
            FileDetailResponse file = fileService.findById(id, actor);
            return toJson(fileDetailWithCircuit(file));
        });
    }

    @Tool(name = "list_file_attachments", description = """
            Liste les pièces jointes d'un dossier (métadonnées uniquement : nom, type, taille, kind).
            Pas de contenu binaire.
            """)
    public String listFileAttachments(
            @ToolParam(description = "UUID du dossier") String fileId) {
        return runTool("list_file_attachments", Map.of("fileId", nz(fileId)), () -> {
            if (!hasPerm(RbacPermissions.FILES_READ)) {
                return errorJson("PERMISSION_DENIED", "Permission FILES:READ requise.");
            }
            UUID id = requireUuid(fileId, "fileId");
            if (id == null) {
                return errorJson("INVALID_ID", "fileId UUID invalide.");
            }
            FileEntity file = fileService.loadForAttachment(id, actor);
            List<FileAttachmentResponse> attachments = fileAttachmentService.listForFile(file);
            List<Map<String, Object>> items = attachments.stream().limit(MAX_LIST).map(a -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", a.id());
                m.put("originalFilename", a.originalFilename());
                m.put("contentType", a.contentType());
                m.put("sizeBytes", a.sizeBytes());
                m.put("kind", a.kind());
                m.put("passageLabel", a.passageLabel());
                m.put("passageResponsibleName", a.passageResponsibleName());
                m.put("uploadedByName", a.uploadedByName());
                m.put("createdAt", a.createdAt());
                return m;
            }).toList();
            ctx.addFileCitation(file.getId(), file.getReferenceNumber());
            return toJson(Map.of(
                    "fileId", file.getId(),
                    "referenceNumber", file.getReferenceNumber(),
                    "count", attachments.size(),
                    "items", items));
        });
    }

    // --- Passation ---

    @Tool(name = "list_passages", description = """
            Historique / circuit de passation d'un dossier (étapes, responsables, statuts, retards).
            """)
    public String listPassages(
            @ToolParam(description = "UUID du dossier") String fileId) {
        return runTool("list_passages", Map.of("fileId", nz(fileId)), () -> {
            if (!hasPerm(RbacPermissions.FILES_READ)) {
                return errorJson("PERMISSION_DENIED", "Permission FILES:READ requise.");
            }
            UUID id = requireUuid(fileId, "fileId");
            if (id == null) {
                return errorJson("INVALID_ID", "fileId UUID invalide.");
            }
            FilePassageCircuitResponse circuit = passageService.getCircuit(id, actor);
            Map<String, Object> payload = circuitOverview(circuit);
            List<Map<String, Object>> passages = new ArrayList<>();
            if (circuit.passages() != null) {
                for (PassageResponse p : circuit.passages()) {
                    passages.add(passageMap(p));
                }
            }
            payload.put("passages", passages);
            return toJson(payload);
        });
    }

    @Tool(name = "get_current_passage", description = """
            Possessionnaire / maillon actuel d'un dossier (qui traite, depuis quand, échéance, retard).
            """)
    public String getCurrentPassage(
            @ToolParam(description = "UUID du dossier") String fileId) {
        return runTool("get_current_passage", Map.of("fileId", nz(fileId)), () -> {
            if (!hasPerm(RbacPermissions.FILES_READ)) {
                return errorJson("PERMISSION_DENIED", "Permission FILES:READ requise.");
            }
            UUID id = requireUuid(fileId, "fileId");
            if (id == null) {
                return errorJson("INVALID_ID", "fileId UUID invalide.");
            }
            CurrentHolderResponse holder = passageService.getCurrentHolder(id, actor);
            Map<String, Object> payload = toHolderMap(holder);
            payload.put("fileId", id);
            payload.put("uiPath", "/files/" + id);
            return toJson(payload);
        });
    }

    // --- Alertes / notifications ---

    @Tool(name = "list_file_alerts", description = """
            Alertes générées sur un dossier (type, escalade, message, dates).
            """)
    public String listFileAlerts(
            @ToolParam(description = "UUID du dossier") String fileId) {
        return runTool("list_file_alerts", Map.of("fileId", nz(fileId)), () -> {
            if (!hasPerm(RbacPermissions.FILES_READ)) {
                return errorJson("PERMISSION_DENIED", "Permission FILES:READ requise.");
            }
            UUID id = requireUuid(fileId, "fileId");
            if (id == null) {
                return errorJson("INVALID_ID", "fileId UUID invalide.");
            }
            // assert access via load
            fileService.loadForAttachment(id, actor);
            List<Alert> alerts = notificationService.listForFile(id);
            List<Map<String, Object>> items = alerts.stream()
                    .limit(MAX_LIST)
                    .map(AlertMapper::toResponse)
                    .map(this::alertMap)
                    .toList();
            return toJson(Map.of("fileId", id, "count", alerts.size(), "items", items));
        });
    }

    @Tool(name = "list_my_notifications", description = """
            Notifications in-app de l'utilisateur connecté.
            unreadOnly=true pour non lues uniquement. limit max 10.
            """)
    public String listMyNotifications(
            @ToolParam(required = false, description = "true = non lues seulement") Boolean unreadOnly,
            @ToolParam(required = false, description = "Nombre max (1-10)") Integer limit) {
        Map<String, Object> args = new LinkedHashMap<>();
        args.put("unreadOnly", unreadOnly);
        args.put("limit", limit);
        return runTool("list_my_notifications", args, () -> {
            boolean unread = unreadOnly == null || unreadOnly;
            int size = limit == null ? MAX_LIST : Math.max(1, Math.min(MAX_LIST, limit));
            var page = notificationService.listForUser(
                    actor.getId(), unread, PageRequest.of(0, size));
            List<Map<String, Object>> items = page.getContent().stream()
                    .map(AlertMapper::toResponse)
                    .map(this::alertMap)
                    .toList();
            for (AlertResponse a : page.getContent().stream().map(AlertMapper::toResponse).toList()) {
                if (a.fileId() != null) {
                    ctx.addFileCitation(a.fileId(), a.fileReferenceNumber());
                }
            }
            return toJson(Map.of(
                    "unreadOnly", unread,
                    "totalElements", page.getTotalElements(),
                    "unreadCount", notificationService.countUnread(actor.getId()),
                    "items", items));
        });
    }

    // --- Dashboard ---

    @Tool(name = "get_my_activity", description = """
            Charge personnelle : dossiers/maillons en cours et en retard pour l'utilisateur connecté.
            """)
    public String getMyActivity() {
        return runTool("get_my_activity", Map.of(), () -> {
            if (!hasPerm(RbacPermissions.DASHBOARD_READ)) {
                return errorJson("PERMISSION_DENIED", "Permission DASHBOARD:READ requise.");
            }
            MyActivityResponse activity = dashboardService.myActivity(actor);
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("activeCount", activity.activeCount());
            payload.put("overdueCount", activity.overdueCount());
            payload.put("transmittedRecentCount", activity.transmittedRecentCount());
            List<Map<String, Object>> items = new ArrayList<>();
            if (activity.items() != null) {
                activity.items().stream().limit(MAX_LIST).forEach(i -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("fileId", i.fileId());
                    m.put("fileReferenceNumber", i.fileReferenceNumber());
                    m.put("fileSubject", i.fileSubject());
                    m.put("stepLabel", i.stepLabel());
                    m.put("receivedAt", i.receivedAt());
                    m.put("dueAt", i.dueAt());
                    m.put("overdue", i.overdue());
                    m.put("uiPath", "/files/" + i.fileId());
                    items.add(m);
                    ctx.addFileCitation(i.fileId(), i.fileReferenceNumber());
                });
            }
            payload.put("items", items);
            payload.put("uiPath", "/dashboard");
            ctx.addCitation("dashboard", null, "Mon activité", "/dashboard");
            return toJson(payload);
        });
    }

    @Tool(name = "get_overdue_files", description = """
            Top dossiers en retard dans le périmètre (limit 1-10, défaut 10).
            organizationId optionnel (UUID) pour filtrer.
            """)
    public String getOverdueFiles(
            @ToolParam(required = false, description = "UUID organisation") String organizationId,
            @ToolParam(required = false, description = "Limite 1-10") Integer limit) {
        Map<String, Object> args = new LinkedHashMap<>();
        args.put("organizationId", organizationId);
        args.put("limit", limit);
        return runTool("get_overdue_files", args, () -> {
            if (!hasPerm(RbacPermissions.DASHBOARD_READ)) {
                return errorJson("PERMISSION_DENIED", "Permission DASHBOARD:READ requise.");
            }
            int lim = limit == null ? MAX_LIST : Math.max(1, Math.min(MAX_LIST, limit));
            List<OverdueFileResponse> rows = dashboardService.overdueFiles(
                    actor, parseUuid(organizationId), lim);
            List<Map<String, Object>> items = new ArrayList<>();
            StringBuilder list = new StringBuilder();
            for (OverdueFileResponse r : rows) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("fileId", r.fileId());
                m.put("referenceNumber", r.referenceNumber());
                m.put("subject", r.subject());
                m.put("fileTypeCode", r.fileTypeCode());
                m.put("organizationCode", r.organizationCode());
                m.put("stepLabel", r.stepLabel());
                m.put("responsibleUserName", r.responsibleUserName());
                m.put("dueAt", r.dueAt());
                m.put("daysOverdue", r.daysOverdue());
                m.put("uiPath", "/files/" + r.fileId());
                items.add(m);
                list.append("- **").append(escapeMd(nz(r.referenceNumber()))).append("** — ")
                        .append(r.daysOverdue()).append(" j de retard")
                        .append(" · ").append(escapeMd(nz(r.organizationCode())))
                        .append(" · ").append(escapeMd(nz(r.stepLabel())));
                if (r.responsibleUserName() != null && !r.responsibleUserName().isBlank()) {
                    list.append(" · ").append(escapeMd(r.responsibleUserName()));
                }
                list.append("\n  ").append(escapeMd(truncate(nz(r.subject()), 80))).append("\n");
                ctx.addFileCitation(r.fileId(), r.referenceNumber());
            }
            ctx.addCitation("dashboard", null, "Retards", "/dashboard/overdue");
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("count", items.size());
            payload.put("items", items);
            payload.put("listMarkdown", list.toString().trim());
            payload.put("preferredFormat", "listMarkdown");
            payload.put("uiPath", "/dashboard/overdue");
            return toJson(payload);        });
    }

    @Tool(name = "get_dashboard_summary", description = """
            Synthèse KPI du périmètre : actifs, retards, clôturés/créés ce mois.
            Filtres optionnels organizationId, fileTypeCode.
            """)
    public String getDashboardSummary(
            @ToolParam(required = false, description = "UUID organisation") String organizationId,
            @ToolParam(required = false, description = "Code type de dossier") String fileTypeCode) {
        Map<String, Object> args = new LinkedHashMap<>();
        args.put("organizationId", organizationId);
        args.put("fileTypeCode", fileTypeCode);
        return runTool("get_dashboard_summary", args, () -> {
            if (!hasPerm(RbacPermissions.DASHBOARD_READ)) {
                return errorJson("PERMISSION_DENIED", "Permission DASHBOARD:READ requise.");
            }
            DashboardSummaryResponse summary = dashboardService.summary(
                    actor, parseUuid(organizationId), blankToNull(fileTypeCode));
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("organizationId", summary.organizationId());
            payload.put("organizationCode", summary.organizationCode());
            payload.put("scopeWidth", summary.scopeWidth());
            payload.put("activeFiles", summary.activeFiles());
            payload.put("overdueFiles", summary.overdueFiles());
            payload.put("closedThisMonth", summary.closedThisMonth());
            payload.put("createdThisMonth", summary.createdThisMonth());
            payload.put("uiPath", "/dashboard");
            ctx.addCitation("dashboard", null, "Tableau de bord", "/dashboard");
            return toJson(payload);
        });
    }

    @Tool(name = "get_workload", description = """
            Charge par agent (actifs / retards) dans le périmètre. organizationId optionnel.
            Retourne aussi une table textuelle pour affichage.
            """)
    public String getWorkload(
            @ToolParam(required = false, description = "UUID organisation") String organizationId) {
        return runTool("get_workload", Map.of("organizationId", nz(organizationId)), () -> {
            if (!hasPerm(RbacPermissions.DASHBOARD_READ)) {
                return errorJson("PERMISSION_DENIED", "Permission DASHBOARD:READ requise.");
            }
            List<WorkloadEntryResponse> rows = dashboardService.workload(actor, parseUuid(organizationId));
            List<Map<String, Object>> items = new ArrayList<>();
            StringBuilder list = new StringBuilder();
            StringBuilder table = new StringBuilder("| Agent | Org | Actifs | Retards |\n");
            table.append("| --- | --- | ---: | ---: |\n");
            for (WorkloadEntryResponse r : rows.stream().limit(MAX_LIST).toList()) {
                Map<String, Object> m = new LinkedHashMap<>();
                String name = (nz(r.firstName()) + " " + nz(r.lastName())).trim();
                m.put("userId", r.userId());
                m.put("name", name);
                m.put("organizationCode", r.organizationCode());
                m.put("activeCount", r.activeCount());
                m.put("overdueCount", r.overdueCount());
                items.add(m);
                list.append("- **").append(escapeMd(name)).append("** (")
                        .append(escapeMd(nz(r.organizationCode())))
                        .append(") — actifs : ").append(r.activeCount())
                        .append(", retards : ").append(r.overdueCount()).append("\n");
                table.append("| ").append(escapeMd(name))
                        .append(" | ").append(escapeMd(nz(r.organizationCode())))
                        .append(" | ").append(r.activeCount())
                        .append(" | ").append(r.overdueCount()).append(" |\n");
            }
            ctx.addCitation("dashboard", null, "Charge", "/dashboard/workload");
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("total", rows.size());
            payload.put("items", items);
            payload.put("listMarkdown", list.toString().trim());
            payload.put("tableMarkdown", table.toString().trim());
            payload.put("preferredFormat", "listMarkdown");
            payload.put("uiPath", "/dashboard/workload");
            return toJson(payload);
        });
    }

    @Tool(name = "get_delay_by_type", description = """
            Délai moyen de traitement par type de dossier (fenêtre en jours, défaut 30, max 365).
            """)
    public String getDelayByType(
            @ToolParam(required = false, description = "UUID organisation") String organizationId,
            @ToolParam(required = false, description = "Fenêtre jours 1-365") Integer windowDays) {
        Map<String, Object> args = new LinkedHashMap<>();
        args.put("organizationId", organizationId);
        args.put("windowDays", windowDays);
        return runTool("get_delay_by_type", args, () -> {
            if (!hasPerm(RbacPermissions.DASHBOARD_READ)) {
                return errorJson("PERMISSION_DENIED", "Permission DASHBOARD:READ requise.");
            }
            int days = clampWindow(windowDays, 30);
            List<DelayByTypeResponse> rows = dashboardService.delayByType(
                    actor, parseUuid(organizationId), days);
            List<Map<String, Object>> items = new ArrayList<>();
            StringBuilder list = new StringBuilder();
            StringBuilder table = new StringBuilder("| Type | Libellé | Clôturés | Délai moy. | Cible |\n");
            table.append("| --- | --- | ---: | ---: | ---: |\n");
            for (DelayByTypeResponse r : rows.stream().limit(MAX_LIST).toList()) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("fileTypeCode", r.fileTypeCode());
                m.put("fileTypeLabel", r.fileTypeLabel());
                m.put("closedCount", r.closedCount());
                m.put("averageDelayDays", r.averageDelayDays());
                m.put("targetDelayDays", r.targetDelayDays());
                items.add(m);
                list.append("- **").append(escapeMd(nz(r.fileTypeCode()))).append("** (")
                        .append(escapeMd(nz(r.fileTypeLabel())))
                        .append(") — délai moy. ").append(r.averageDelayDays())
                        .append(" j / cible ").append(r.targetDelayDays())
                        .append(" j (").append(r.closedCount()).append(" clôturés)\n");
                table.append("| ").append(escapeMd(nz(r.fileTypeCode())))
                        .append(" | ").append(escapeMd(nz(r.fileTypeLabel())))
                        .append(" | ").append(r.closedCount())
                        .append(" | ").append(r.averageDelayDays())
                        .append(" | ").append(r.targetDelayDays()).append(" |\n");
            }
            ctx.addCitation("dashboard", null, "Rapports", "/rapports");
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("windowDays", days);
            payload.put("items", items);
            payload.put("listMarkdown", list.toString().trim());
            payload.put("tableMarkdown", table.toString().trim());
            payload.put("preferredFormat", "listMarkdown");
            payload.put("uiPath", "/rapports");
            return toJson(payload);
        });
    }

    @Tool(name = "get_compliance_ranking", description = """
            Classement des structures par taux de conformité aux délais.
            groupByTypeCode défaut DIRECTORATE ; windowDays défaut 90.
            """)
    public String getComplianceRanking(
            @ToolParam(required = false, description = "Code type org (ex. DIRECTORATE)") String groupByTypeCode,
            @ToolParam(required = false, description = "Fenêtre jours 1-365") Integer windowDays) {
        Map<String, Object> args = new LinkedHashMap<>();
        args.put("groupByTypeCode", groupByTypeCode);
        args.put("windowDays", windowDays);
        return runTool("get_compliance_ranking", args, () -> {
            if (!hasPerm(RbacPermissions.DASHBOARD_READ)) {
                return errorJson("PERMISSION_DENIED", "Permission DASHBOARD:READ requise.");
            }
            int days = clampWindow(windowDays, 90);
            String group = blankToNull(groupByTypeCode) != null ? groupByTypeCode.trim() : "DIRECTORATE";
            List<OrganizationRankingResponse> rows = dashboardService.complianceRanking(actor, group, days);
            List<Map<String, Object>> items = new ArrayList<>();
            StringBuilder list = new StringBuilder();
            StringBuilder table = new StringBuilder("| Rang | Org | Clôturés | Conformes | Taux |\n");
            table.append("| ---: | --- | ---: | ---: | ---: |\n");
            int rank = 1;
            for (OrganizationRankingResponse r : rows.stream().limit(MAX_LIST).toList()) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("rank", rank);
                m.put("organizationId", r.organizationId());
                m.put("organizationCode", r.organizationCode());
                m.put("organizationName", r.organizationName());
                m.put("closedCount", r.closedCount());
                m.put("compliantCount", r.compliantCount());
                m.put("complianceRate", r.complianceRate());
                items.add(m);
                list.append(rank).append(". **").append(escapeMd(nz(r.organizationCode()))).append("** — ")
                        .append("taux ").append(r.complianceRate())
                        .append(" (").append(r.compliantCount()).append("/")
                        .append(r.closedCount()).append(")\n");
                table.append("| ").append(rank)
                        .append(" | ").append(escapeMd(nz(r.organizationCode())))
                        .append(" | ").append(r.closedCount())
                        .append(" | ").append(r.compliantCount())
                        .append(" | ").append(r.complianceRate()).append(" |\n");
                rank++;
            }
            ctx.addCitation("dashboard", null, "Rapports", "/rapports");
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("groupByTypeCode", group);
            payload.put("windowDays", days);
            payload.put("items", items);
            payload.put("listMarkdown", list.toString().trim());
            payload.put("tableMarkdown", table.toString().trim());
            payload.put("preferredFormat", "listMarkdown");
            payload.put("uiPath", "/rapports");
            return toJson(payload);
        });
    }

    @Tool(name = "get_dashboard_analytics", description = """
            Analyse consolidée : KPI, tendance volume, délais par type, ranking, top retards, charge.
            Fenêtre jours (défaut 90). Préférer ce tool pour une vue « ce mois / période ».
            """)
    public String getDashboardAnalytics(
            @ToolParam(required = false, description = "UUID organisation") String organizationId,
            @ToolParam(required = false, description = "Code type de dossier") String fileTypeCode,
            @ToolParam(required = false, description = "Code type org regroupement") String groupByTypeCode,
            @ToolParam(required = false, description = "Fenêtre jours 1-365") Integer windowDays) {
        Map<String, Object> args = new LinkedHashMap<>();
        args.put("organizationId", organizationId);
        args.put("fileTypeCode", fileTypeCode);
        args.put("groupByTypeCode", groupByTypeCode);
        args.put("windowDays", windowDays);
        return runTool("get_dashboard_analytics", args, () -> {
            if (!hasPerm(RbacPermissions.DASHBOARD_READ)) {
                return errorJson("PERMISSION_DENIED", "Permission DASHBOARD:READ requise.");
            }
            int days = clampWindow(windowDays, 90);
            String group = blankToNull(groupByTypeCode) != null ? groupByTypeCode.trim() : "DIRECTORATE";
            DashboardAnalyticsResponse analytics = dashboardService.analytics(
                    actor,
                    parseUuid(organizationId),
                    blankToNull(fileTypeCode),
                    group,
                    days);
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("organizationCode", analytics.organizationCode());
            payload.put("scopeWidth", analytics.scopeWidth());
            payload.put("windowDays", analytics.windowDays());
            payload.put("granularity", analytics.granularity());
            payload.put("activeFiles", analytics.activeFiles());
            payload.put("overdueFiles", analytics.overdueFiles());
            payload.put("closedInWindow", analytics.closedInWindow());
            payload.put("createdInWindow", analytics.createdInWindow());
            payload.put("complianceRate", analytics.complianceRate());
            payload.put("averageDelayDays", analytics.averageDelayDays());
            if (analytics.volumeTrend() != null) {
                payload.put("volumeTrend", analytics.volumeTrend().stream().limit(MAX_LIST).toList());
            }
            if (analytics.statusBreakdown() != null) {
                payload.put("statusBreakdown", analytics.statusBreakdown());
            }
            if (analytics.delayByType() != null) {
                payload.put("delayByType", analytics.delayByType().stream().limit(MAX_LIST).toList());
            }
            if (analytics.complianceRanking() != null) {
                payload.put("complianceRanking", analytics.complianceRanking().stream().limit(MAX_LIST).toList());
            }
            if (analytics.overdueFilesList() != null) {
                List<Map<String, Object>> overdue = new ArrayList<>();
                for (OverdueFileResponse r : analytics.overdueFilesList().stream().limit(MAX_LIST).toList()) {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("fileId", r.fileId());
                    m.put("referenceNumber", r.referenceNumber());
                    m.put("daysOverdue", r.daysOverdue());
                    m.put("responsibleUserName", r.responsibleUserName());
                    overdue.add(m);
                    ctx.addFileCitation(r.fileId(), r.referenceNumber());
                }
                payload.put("overdueFilesList", overdue);
            }
            if (analytics.workload() != null) {
                payload.put("workload", analytics.workload().stream().limit(MAX_LIST).toList());
            }
            payload.put("uiPath", "/rapports");
            ctx.addCitation("dashboard", null, "Rapports", "/rapports");
            ctx.addCitation("dashboard", null, "Tableau de bord", "/dashboard");
            return toJson(payload);
        });
    }

    // --- helpers ---

    private Map<String, Object> fileDetailWithCircuit(FileDetailResponse file) {
        FilePassageCircuitResponse circuit = passageService.getCircuit(file.id(), actor);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", file.id());
        payload.put("referenceNumber", file.referenceNumber());
        payload.put("subject", file.subject());
        payload.put("senderOrBeneficiary", file.senderOrBeneficiary());
        payload.put("status", enumName(file.status()));
        payload.put("priority", enumName(file.priority()));
        payload.put("fileTypeCode", file.fileTypeCode());
        payload.put("organizationCode", file.organizationCode());
        payload.put("organizationName", file.organizationName());
        payload.put("receivedAt", file.receivedAt());
        payload.put("uiPath", "/files/" + file.id());
        if (circuit != null) {
            payload.putAll(circuitOverview(circuit));
        }
        ctx.addFileCitation(file.id(), file.referenceNumber());
        return payload;
    }

    private Map<String, Object> circuitOverview(FilePassageCircuitResponse circuit) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("templateCode", circuit.templateCode());
        payload.put("templateName", circuit.templateName());
        payload.put("currentStepOrder", circuit.currentStepOrder());
        payload.put("currentHolder", toHolderMap(circuit.currentHolder()));
        if (circuit.currentHolders() != null && !circuit.currentHolders().isEmpty()) {
            payload.put("currentHolders", circuit.currentHolders().stream().map(this::toHolderMap).toList());
        }
        return payload;
    }

    private Map<String, Object> fileSummaryMap(FileSummaryResponse f) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", f.id());
        m.put("referenceNumber", f.referenceNumber());
        m.put("subject", f.subject());
        m.put("status", enumName(f.status()));
        m.put("priority", enumName(f.priority()));
        m.put("fileTypeCode", f.fileTypeCode());
        m.put("organizationCode", f.organizationCode());
        m.put("receivedAt", f.receivedAt());
        m.put("awaitingMyAction", f.awaitingMyAction());
        m.put("myPassageLabel", f.myPassageLabel());
        m.put("uiPath", "/files/" + f.id());
        return m;
    }

    private Map<String, Object> passageMap(PassageResponse p) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", p.id());
        m.put("stepOrder", p.stepOrder());
        m.put("label", p.label());
        m.put("status", enumName(p.status()));
        m.put("responsibleName", p.responsibleName());
        m.put("responsibleOrganizationCode", p.responsibleOrganizationCode());
        m.put("receivedAt", p.receivedAt());
        m.put("dueAt", p.dueAt());
        m.put("overdue", p.overdue());
        m.put("workingDaysHeld", p.workingDaysHeld());
        m.put("returnReason", p.returnReason());
        return m;
    }

    private Map<String, Object> alertMap(AlertResponse a) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", a.id());
        m.put("fileId", a.fileId());
        m.put("fileReferenceNumber", a.fileReferenceNumber());
        m.put("alertTypeCode", a.alertTypeCode());
        m.put("alertTypeLabel", a.alertTypeLabel());
        m.put("escalationLevel", a.escalationLevel());
        m.put("status", enumName(a.status()));
        m.put("message", a.message());
        m.put("sentAt", a.sentAt());
        m.put("readAt", a.readAt());
        if (a.fileId() != null) {
            m.put("uiPath", "/files/" + a.fileId());
        }
        return m;
    }

    private Map<String, Object> toHolderMap(CurrentHolderResponse holder) {
        if (holder == null) {
            return null;
        }
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("userId", holder.userId());
        map.put("fullName", holder.fullName());
        map.put("organizationCode", holder.organizationCode());
        map.put("stepLabel", holder.stepLabel());
        map.put("stepOrder", holder.stepOrder());
        map.put("since", holder.since());
        map.put("workingDaysHeld", holder.workingDaysHeld());
        map.put("overdue", holder.overdue());
        map.put("dueAt", holder.dueAt());
        return map;
    }

    private boolean hasPerm(String permission) {
        return ctx.hasPerm(permission);
    }

    private String runTool(String name, Map<String, ?> args, AssistantToolContext.ToolBody body) {
        return ctx.runTool(name, args, body);
    }

    private String toJson(Object value) {
        return ctx.toJson(value);
    }

    private String errorJson(String code, String message) {
        return ctx.errorJson(code, message);
    }

    private static int clampWindow(Integer windowDays, int defaultDays) {
        if (windowDays == null || windowDays < 1) {
            return defaultDays;
        }
        return Math.min(365, windowDays);
    }

    private static String escapeMd(String v) {
        if (v == null || v.isEmpty()) {
            return "";
        }
        return v.replace("|", "/").replace("\n", " ").trim();
    }

    private static String truncate(String v, int max) {
        if (v.length() <= max) {
            return v;
        }
        return v.substring(0, max - 1) + "…";
    }

    private static String nz(String v) {
        return v == null ? "" : v;
    }

    private static String blankToNull(String v) {
        return v == null || v.isBlank() ? null : v.trim();
    }

    private static UUID parseUuid(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(raw.trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static UUID requireUuid(String raw, String field) {
        return parseUuid(raw);
    }

    private static LocalDate parseDate(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return LocalDate.parse(raw.trim());
    }

    private static <E extends Enum<E>> E parseEnum(Class<E> type, String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return Enum.valueOf(type, raw.trim().toUpperCase());
    }

    private static String enumName(Enum<?> e) {
        return e == null ? null : e.name();
    }

    public record ToolCallTrace(
            String toolName,
            String argumentsJson,
            String resultSummary,
            boolean success,
            int durationMs
    ) {
    }
}
