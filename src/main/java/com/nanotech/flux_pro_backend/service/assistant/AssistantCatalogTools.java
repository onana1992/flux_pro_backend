package com.nanotech.flux_pro_backend.service.assistant;

import com.nanotech.flux_pro_backend.dto.response.AlertRuleResponse;
import com.nanotech.flux_pro_backend.dto.response.AlertTypeResponse;
import com.nanotech.flux_pro_backend.dto.response.BusinessCalendarDayResponse;
import com.nanotech.flux_pro_backend.dto.response.ChainStepTemplateResponse;
import com.nanotech.flux_pro_backend.dto.response.ChainTemplateDetailResponse;
import com.nanotech.flux_pro_backend.dto.response.ChainTemplateSummaryResponse;
import com.nanotech.flux_pro_backend.dto.response.FileTypeResponse;
import com.nanotech.flux_pro_backend.dto.response.OrganizationDetailResponse;
import com.nanotech.flux_pro_backend.dto.response.OrganizationTreeResponse;
import com.nanotech.flux_pro_backend.dto.response.PreconfiguredDossierResponse;
import com.nanotech.flux_pro_backend.dto.response.RoleSummaryResponse;
import com.nanotech.flux_pro_backend.dto.response.UserResponse;
import com.nanotech.flux_pro_backend.entity.AlertRule;
import com.nanotech.flux_pro_backend.entity.AlertType;
import com.nanotech.flux_pro_backend.entity.BusinessCalendarDay;
import com.nanotech.flux_pro_backend.entity.ChainTemplate;
import com.nanotech.flux_pro_backend.entity.FileType;
import com.nanotech.flux_pro_backend.entity.PreconfiguredDossier;
import com.nanotech.flux_pro_backend.enumeration.UserRole;
import com.nanotech.flux_pro_backend.mapper.AlertRuleMapper;
import com.nanotech.flux_pro_backend.mapper.AlertTypeMapper;
import com.nanotech.flux_pro_backend.mapper.BusinessCalendarDayMapper;
import com.nanotech.flux_pro_backend.mapper.ChainTemplateMapper;
import com.nanotech.flux_pro_backend.mapper.FileTypeMapper;
import com.nanotech.flux_pro_backend.mapper.PreconfiguredDossierMapper;
import com.nanotech.flux_pro_backend.security.AccessControlService;
import com.nanotech.flux_pro_backend.security.OrganizationScopeService;
import com.nanotech.flux_pro_backend.security.RbacPermissions;
import com.nanotech.flux_pro_backend.security.TranslatableAccessDeniedException;
import com.nanotech.flux_pro_backend.service.AlertRuleService;
import com.nanotech.flux_pro_backend.service.AlertTypeService;
import com.nanotech.flux_pro_backend.service.BusinessCalendarDayService;
import com.nanotech.flux_pro_backend.service.ChainTemplateService;
import com.nanotech.flux_pro_backend.service.FileTypeService;
import com.nanotech.flux_pro_backend.service.OrganizationService;
import com.nanotech.flux_pro_backend.service.PreconfiguredDossierService;
import com.nanotech.flux_pro_backend.service.UserService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Tools Sprint 3 — org, utilisateurs, référentiels, aide UI.
 */
public class AssistantCatalogTools {

    private static final int MAX_LIST = 10;
    private static final int MAX_ORG_USERS = 25;

    private final AssistantToolContext ctx;
    private final OrganizationService organizationService;
    private final OrganizationScopeService organizationScopeService;
    private final AccessControlService accessControlService;
    private final UserService userService;
    private final FileTypeService fileTypeService;
    private final ChainTemplateService chainTemplateService;
    private final AlertTypeService alertTypeService;
    private final AlertRuleService alertRuleService;
    private final BusinessCalendarDayService businessCalendarDayService;
    private final PreconfiguredDossierService preconfiguredDossierService;
    private final AssistantHelpKnowledgeBase helpKnowledgeBase;

    public AssistantCatalogTools(
            AssistantToolContext ctx,
            OrganizationService organizationService,
            OrganizationScopeService organizationScopeService,
            AccessControlService accessControlService,
            UserService userService,
            FileTypeService fileTypeService,
            ChainTemplateService chainTemplateService,
            AlertTypeService alertTypeService,
            AlertRuleService alertRuleService,
            BusinessCalendarDayService businessCalendarDayService,
            PreconfiguredDossierService preconfiguredDossierService,
            AssistantHelpKnowledgeBase helpKnowledgeBase) {
        this.ctx = ctx;
        this.organizationService = organizationService;
        this.organizationScopeService = organizationScopeService;
        this.accessControlService = accessControlService;
        this.userService = userService;
        this.fileTypeService = fileTypeService;
        this.chainTemplateService = chainTemplateService;
        this.alertTypeService = alertTypeService;
        this.alertRuleService = alertRuleService;
        this.businessCalendarDayService = businessCalendarDayService;
        this.preconfiguredDossierService = preconfiguredDossierService;
        this.helpKnowledgeBase = helpKnowledgeBase;
    }

    @Tool(name = "get_organization_tree", description = """
            Arborescence organisations accessibles (périmètre RBAC).
            Utile pour « quelles sous-structures sous X ? » si tu n'as pas encore le code exact.
            """)
    public String getOrganizationTree() {
        return ctx.runTool("get_organization_tree", Map.of(), () -> {
            List<OrganizationTreeResponse> tree = organizationService.getTree(ctx.actor());
            List<Map<String, Object>> roots = tree.stream().map(this::orgNode).toList();
            ctx.addCitation("org", null, "Organigramme", "/admin/org");
            return ctx.toJson(Map.of("roots", roots, "uiPath", "/admin/org"));
        });
    }

    @Tool(name = "get_my_organization", description = """
            Organisation de l'utilisateur connecté : détail + chemin hiérarchique (racine → org).
            Questions : « Quelle est mon organisation ? », « Où suis-je dans l'organigramme ? ».
            """)
    public String getMyOrganization() {
        return ctx.runTool("get_my_organization", Map.of(), () -> {
            var profile = userService.getMeProfile(ctx.actor());
            if (profile.organization() == null || profile.organization().id() == null) {
                return ctx.errorJson("NO_ORGANIZATION", "Aucune organisation associée au profil.");
            }
            UUID orgId = profile.organization().id();
            OrganizationDetailResponse org = organizationService.getDetailById(orgId, ctx.actor());
            Map<String, Object> payload = orgDetailMap(org);
            payload.put("hierarchyPath", buildHierarchyPath(org));
            payload.put("listMarkdown", "- **" + escapeMd(org.code()) + "** — " + escapeMd(org.name())
                    + " (" + escapeMd(nz(org.typeName())) + ")");
            ctx.addCitation("org", org.id(), org.code(), "/admin/org/" + org.id());
            return ctx.toJson(payload);
        });
    }

    @Tool(name = "get_organization", description = """
            Détail d'une organisation par UUID (organizationId) OU par code (ex. DAG, DIER, DSI).
            Fournir au moins un des deux.
            """)
    public String getOrganization(
            @ToolParam(required = false, description = "UUID organisation") String organizationId,
            @ToolParam(required = false, description = "Code organisation (ex. DAG)") String code) {
        Map<String, Object> args = new LinkedHashMap<>();
        args.put("organizationId", organizationId);
        args.put("code", code);
        return ctx.runTool("get_organization", args, () -> {
            OrganizationDetailResponse org = resolveOrganization(organizationId, code);
            if (org == null) {
                return ctx.errorJson("INVALID_ARGS",
                        "Fournir organizationId (UUID) ou code organisation valide.");
            }
            Map<String, Object> payload = orgDetailMap(org);
            payload.put("hierarchyPath", buildHierarchyPath(org));
            ctx.addCitation("org", org.id(), org.code(), "/admin/org/" + org.id());
            return ctx.toJson(payload);
        });
    }

    @Tool(name = "get_organization_children", description = """
            Sous-structures directes sous une organisation (par id ou code, ex. « sous la DAG »).
            """)
    public String getOrganizationChildren(
            @ToolParam(required = false, description = "UUID organisation parente") String organizationId,
            @ToolParam(required = false, description = "Code organisation parente") String code) {
        Map<String, Object> args = new LinkedHashMap<>();
        args.put("organizationId", organizationId);
        args.put("code", code);
        return ctx.runTool("get_organization_children", args, () -> {
            OrganizationDetailResponse parent = resolveOrganization(organizationId, code);
            if (parent == null) {
                return ctx.errorJson("INVALID_ARGS", "Fournir organizationId ou code de la structure parente.");
            }
            OrganizationTreeResponse node = findInTree(organizationService.getTree(ctx.actor()), parent.id());
            List<Map<String, Object>> children = new ArrayList<>();
            StringBuilder list = new StringBuilder();
            if (node != null && node.children() != null) {
                for (OrganizationTreeResponse child : node.children().stream().limit(MAX_LIST).toList()) {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", child.id());
                    m.put("code", child.code());
                    m.put("name", child.name());
                    if (child.type() != null) {
                        m.put("typeCode", child.type().code());
                        m.put("typeName", child.type().name());
                    }
                    m.put("active", child.active());
                    m.put("childCount", child.children() != null ? child.children().size() : 0);
                    m.put("uiPath", "/admin/org/" + child.id());
                    children.add(m);
                    list.append("- **").append(escapeMd(child.code())).append("** — ")
                            .append(escapeMd(child.name()));
                    if (child.type() != null) {
                        list.append(" (").append(escapeMd(nz(child.type().name()))).append(")");
                    }
                    list.append("\n");
                }
            }
            ctx.addCitation("org", parent.id(), parent.code(), "/admin/org/" + parent.id());
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("parentId", parent.id());
            payload.put("parentCode", parent.code());
            payload.put("parentName", parent.name());
            payload.put("count", children.size());
            payload.put("children", children);
            payload.put("listMarkdown", list.toString().trim());
            payload.put("preferredFormat", "listMarkdown");
            payload.put("uiPath", "/admin/org/" + parent.id());
            return ctx.toJson(payload);
        });
    }

    @Tool(name = "list_organization_users", description = """
            Agents / utilisateurs rattachés à une organisation (id ou code).
            Mettre includeDescendants=true pour inclure toutes les sous-structures
            (ex. « utilisateurs de DAG et ses descendants ») — UN seul appel suffit, ne pas boucler.
            Permission USERS:READ (ou rôle directeur/chef) requise. Max 25.
            """)
    public String listOrganizationUsers(
            @ToolParam(required = false, description = "UUID organisation") String organizationId,
            @ToolParam(required = false, description = "Code organisation (ex. DAG)") String code,
            @ToolParam(required = false, description = "Filtrer rôle UserRole") String role,
            @ToolParam(required = false, description = "true = inclure les descendants") Boolean includeDescendants) {
        Map<String, Object> args = new LinkedHashMap<>();
        args.put("organizationId", organizationId);
        args.put("code", code);
        args.put("role", role);
        args.put("includeDescendants", includeDescendants);
        return ctx.runTool("list_organization_users", args, () -> {
            if (!canListUsers()) {
                return ctx.errorJson("PERMISSION_DENIED",
                        "Accès utilisateurs refusé : permission USERS:READ ou rôle de direction/chef requis.");
            }
            OrganizationDetailResponse org = resolveOrganization(organizationId, code);
            if (org == null) {
                return ctx.errorJson("INVALID_ARGS", "Fournir organizationId ou code organisation.");
            }
            UserRole parsedRole = parseRole(role);
            if (role != null && !role.isBlank() && parsedRole == null) {
                return ctx.errorJson("INVALID_ROLE", "Rôle inconnu : " + role);
            }
            try {
                boolean withDescendants = Boolean.TRUE.equals(includeDescendants);
                Page<UserResponse> page;
                if (withDescendants) {
                    var orgIds = organizationScopeService.collectSelfAndDescendants(org.id());
                    page = userService.search(
                            ctx.actor(),
                            null,
                            orgIds,
                            parsedRole,
                            null,
                            PageRequest.of(0, MAX_ORG_USERS));
                } else {
                    page = userService.search(
                            ctx.actor(),
                            org.id(),
                            parsedRole,
                            null,
                            PageRequest.of(0, MAX_ORG_USERS));
                }
                Map<String, Object> payload = usersPagePayload(page, org.code(), "/admin/users");
                payload.put("includeDescendants", withDescendants);
                payload.put("rootOrganizationCode", org.code());
                if (withDescendants) {
                    payload.put("hint",
                            "Résultats = utilisateurs de " + org.code()
                                    + " et de ses sous-structures (plafonnés à " + MAX_ORG_USERS + ").");
                }
                ctx.addCitation("org", org.id(), org.code(), "/admin/org/" + org.id());
                return ctx.toJson(payload);
            } catch (TranslatableAccessDeniedException ex) {
                return ctx.errorJson("PERMISSION_DENIED",
                        "Accès refusé pour lister les utilisateurs de " + org.code()
                                + " (hors périmètre ou droit insuffisant).");
            }
        });
    }

    @Tool(name = "get_organization_heads", description = """
            Responsables / chefs d'organisation (flag organizationHead) pour une structure (id ou code).
            Permission USERS:READ requise.
            """)
    public String getOrganizationHeads(
            @ToolParam(required = false, description = "UUID organisation") String organizationId,
            @ToolParam(required = false, description = "Code organisation") String code) {
        Map<String, Object> args = new LinkedHashMap<>();
        args.put("organizationId", organizationId);
        args.put("code", code);
        return ctx.runTool("get_organization_heads", args, () -> {
            if (!canListUsers()) {
                return ctx.errorJson("PERMISSION_DENIED",
                        "Accès utilisateurs refusé : permission USERS:READ ou rôle de direction/chef requis.");
            }
            OrganizationDetailResponse org = resolveOrganization(organizationId, code);
            if (org == null) {
                return ctx.errorJson("INVALID_ARGS", "Fournir organizationId ou code organisation.");
            }
            // page plus large puis filtre chefs
            try {
                Page<UserResponse> page = userService.search(
                        ctx.actor(), org.id(), null, null, PageRequest.of(0, 50));
                List<UserResponse> heads = page.getContent().stream()
                        .filter(UserResponse::organizationHead)
                        .limit(MAX_LIST)
                        .toList();
                StringBuilder list = new StringBuilder();
                List<Map<String, Object>> items = new ArrayList<>();
                for (UserResponse u : heads) {
                    items.add(userDetailMap(u));
                    list.append("- **").append(escapeMd(displayName(u))).append("** — ")
                            .append(u.role() != null ? u.role().name() : "?");
                    if (u.jobTitle() != null && !u.jobTitle().isBlank()) {
                        list.append(" · ").append(escapeMd(u.jobTitle()));
                    }
                    list.append("\n");
                }
                ctx.addCitation("org", org.id(), org.code(), "/admin/org/" + org.id());
                ctx.addCitation("users", null, "Utilisateurs", "/admin/users");
                Map<String, Object> payload = new LinkedHashMap<>();
                payload.put("organizationId", org.id());
                payload.put("organizationCode", org.code());
                payload.put("count", items.size());
                payload.put("items", items);
                payload.put("listMarkdown", list.toString().trim());
                payload.put("preferredFormat", "listMarkdown");
                if (items.isEmpty()) {
                    payload.put("hint", "Aucun utilisateur marqué chef d'organisation pour cette structure.");
                }
                payload.put("uiPath", "/admin/users");
                return ctx.toJson(payload);
            } catch (TranslatableAccessDeniedException ex) {
                return ctx.errorJson("PERMISSION_DENIED",
                        "Accès refusé pour lister les chefs de " + org.code() + ".");
            }
        });
    }

    @Tool(name = "get_user", description = """
            Fiche détaillée d'un utilisateur par UUID (userId).
            Inclut rôle, org, poste, chef d'org, suppléant, actif.
            Permission USERS:READ requise.
            """)
    public String getUser(
            @ToolParam(description = "UUID utilisateur") String userId) {
        return ctx.runTool("get_user", Map.of("userId", nz(userId)), () -> {
            if (!canListUsers()) {
                return ctx.errorJson("PERMISSION_DENIED",
                        "Accès utilisateurs refusé : permission USERS:READ ou rôle de direction/chef requis.");
            }
            UUID id = parseUuid(userId);
            if (id == null) {
                return ctx.errorJson("INVALID_ID", "userId UUID invalide.");
            }
            UserResponse user = userService.getById(ctx.actor(), id);
            Map<String, Object> payload = userDetailMap(user);
            payload.put("listMarkdown", formatUserListLine(user));
            ctx.addCitation("users", user.id(), displayName(user), "/admin/users/" + user.id());
            return ctx.toJson(payload);
        });
    }

    @Tool(name = "search_users", description = """
            Recherche utilisateurs (max 10) par nom, prénom, email, matricule.
            Accepte le nom complet (« Roger NSANGOU ») ou un seul mot (« NSANGOU »).
            Filtres optionnels : role, organizationId, organizationCode.
            Permission USERS:READ requise.
            Si 0 résultat : le dire clairement (pas inventer). Si PERMISSION_DENIED : expliquer le besoin de USERS:READ.
            """)
    public String searchUsers(
            @ToolParam(required = false, description = "Texte (nom, email, matricule)") String search,
            @ToolParam(required = false, description = "Rôle UserRole") String role,
            @ToolParam(required = false, description = "UUID organisation") String organizationId,
            @ToolParam(required = false, description = "Code organisation") String organizationCode) {
        Map<String, Object> args = new LinkedHashMap<>();
        args.put("search", search);
        args.put("role", role);
        args.put("organizationId", organizationId);
        args.put("organizationCode", organizationCode);
        return ctx.runTool("search_users", args, () -> {
            if (!canListUsers()) {
                return ctx.errorJson("PERMISSION_DENIED",
                        "Accès utilisateurs refusé : permission USERS:READ ou rôle de direction/chef requis. Voir /profile.");
            }
            UserRole parsedRole = parseRole(role);
            if (role != null && !role.isBlank() && parsedRole == null) {
                return ctx.errorJson("INVALID_ROLE", "Rôle inconnu : " + role);
            }
            UUID orgId = parseUuid(organizationId);
            if (orgId == null && blankToNull(organizationCode) != null) {
                OrganizationDetailResponse org = resolveOrganization(null, organizationCode);
                if (org == null) {
                    return ctx.errorJson("ORGANIZATION_NOT_FOUND",
                            "Organisation introuvable : " + organizationCode);
                }
                orgId = org.id();
            }
            return usersPageJson(
                    userService.search(
                            ctx.actor(), orgId, parsedRole, blankToNull(search), PageRequest.of(0, MAX_LIST)),
                    null,
                    "/admin/users");
        });
    }

    @Tool(name = "list_file_types", description = """
            Liste les types de dossiers actifs (code, libellé).
            """)
    public String listFileTypes() {
        return ctx.runTool("list_file_types", Map.of(), () -> {
            List<FileTypeResponse> types = fileTypeService.listActive().stream()
                    .map(FileTypeMapper::toResponse)
                    .limit(50)
                    .toList();
            List<Map<String, Object>> items = types.stream().map(t -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", t.id());
                m.put("code", t.code());
                m.put("name", t.name());
                m.put("directionCode", t.directionCode());
                return m;
            }).toList();
            String href = ctx.hasPerm(RbacPermissions.FILE_TYPES_READ)
                    ? "/admin/file-types" : "/files/new";
            ctx.addCitation("referential", null, "Types de dossiers", href);
            return ctx.toJson(Map.of("count", items.size(), "items", items, "uiPath", href));
        });
    }

    @Tool(name = "list_chain_templates", description = """
            Liste les templates de circuit (max 10). Filtres : active, fileTypeCode, search.
            Permission CHAIN_TEMPLATES:READ requise.
            """)
    public String listChainTemplates(
            @ToolParam(required = false, description = "true/false actifs seulement") Boolean active,
            @ToolParam(required = false, description = "Code type dossier") String fileTypeCode,
            @ToolParam(required = false, description = "Recherche texte") String search) {
        Map<String, Object> args = new LinkedHashMap<>();
        args.put("active", active);
        args.put("fileTypeCode", fileTypeCode);
        args.put("search", search);
        return ctx.runTool("list_chain_templates", args, () -> {
            if (!ctx.hasPerm(RbacPermissions.CHAIN_TEMPLATES_READ)) {
                return ctx.errorJson("PERMISSION_DENIED",
                        "Permission CHAIN_TEMPLATES:READ requise pour consulter les templates. "
                                + "Demandez à un administrateur métier. Écran : /admin/chain-templates");
            }
            Page<ChainTemplateSummaryResponse> page = chainTemplateService.findAllSummaries(
                    active, blankToNull(fileTypeCode), blankToNull(search), PageRequest.of(0, MAX_LIST));
            List<Map<String, Object>> items = new ArrayList<>();
            for (ChainTemplateSummaryResponse t : page.getContent()) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", t.id());
                m.put("code", t.code());
                m.put("name", t.name());
                m.put("fileTypeCode", t.fileTypeCode());
                m.put("totalDelayDays", t.totalDelayDays());
                m.put("stepCount", t.stepCount());
                m.put("active", t.active());
                m.put("uiPath", "/admin/chain-templates/" + t.id());
                items.add(m);
            }
            ctx.addCitation("referential", null, "Templates", "/admin/chain-templates");
            return ctx.toJson(Map.of(
                    "totalElements", page.getTotalElements(),
                    "items", items,
                    "uiPath", "/admin/chain-templates"));
        });
    }

    @Tool(name = "get_chain_template", description = """
            Détail d'un template de circuit par id (UUID) OU par code (ex. T01), avec ses étapes.
            Permission CHAIN_TEMPLATES:READ requise.
            """)
    public String getChainTemplate(
            @ToolParam(required = false, description = "UUID template") String templateId,
            @ToolParam(required = false, description = "Code template") String code) {
        Map<String, Object> args = new LinkedHashMap<>();
        args.put("templateId", templateId);
        args.put("code", code);
        return ctx.runTool("get_chain_template", args, () -> {
            if (!ctx.hasPerm(RbacPermissions.CHAIN_TEMPLATES_READ)) {
                return ctx.errorJson("PERMISSION_DENIED",
                        "Permission CHAIN_TEMPLATES:READ requise. "
                                + "Sans ce droit vous ne pouvez pas lire les étapes d'un template. "
                                + "Contactez un admin métier.");
            }
            ChainTemplate entity;
            if (templateId != null && !templateId.isBlank()) {
                UUID id = parseUuid(templateId);
                if (id == null) {
                    return ctx.errorJson("INVALID_ID", "templateId UUID invalide.");
                }
                entity = chainTemplateService.findById(id);
            } else if (code != null && !code.isBlank()) {
                entity = chainTemplateService.findByCode(code.trim());
            } else {
                return ctx.errorJson("INVALID_ARGS", "Fournir templateId ou code.");
            }
            ChainTemplateDetailResponse detail = ChainTemplateMapper.toDetail(entity);
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("id", detail.id());
            payload.put("code", detail.code());
            payload.put("name", detail.name());
            payload.put("description", detail.description());
            payload.put("fileTypeCode", detail.fileTypeCode());
            payload.put("totalDelayDays", detail.totalDelayDays());
            payload.put("delayUnit", detail.delayUnit());
            payload.put("active", detail.active());
            List<Map<String, Object>> steps = new ArrayList<>();
            if (detail.steps() != null) {
                for (ChainStepTemplateResponse s : detail.steps()) {
                    Map<String, Object> step = new LinkedHashMap<>();
                    step.put("stepOrder", s.stepOrder());
                    step.put("label", s.label());
                    step.put("responsibleRole", s.responsibleRole());
                    step.put("organizationCode", s.organizationCode());
                    step.put("delayValue", s.delayValue());
                    step.put("delayUnit", s.delayUnit());
                    step.put("optional", s.optional());
                    step.put("closureStep", s.closureStep());
                    steps.add(step);
                }
            }
            payload.put("steps", steps);
            payload.put("uiPath", "/admin/chain-templates/" + detail.id());
            ctx.addCitation("referential", detail.id(), detail.code(),
                    "/admin/chain-templates/" + detail.id());
            return ctx.toJson(payload);
        });
    }

    @Tool(name = "list_alert_types", description = """
            Types d'alertes actifs (code, libellé). Accessible à tout utilisateur authentifié.
            """)
    public String listAlertTypes() {
        return ctx.runTool("list_alert_types", Map.of(), () -> {
            List<AlertTypeResponse> types = alertTypeService.listActive().stream()
                    .map(AlertTypeMapper::toResponse)
                    .limit(50)
                    .toList();
            List<Map<String, Object>> items = types.stream().map(t -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("code", t.code());
                m.put("label", t.label());
                m.put("description", t.description());
                return m;
            }).toList();
            String href = ctx.hasPerm(RbacPermissions.ALERT_TYPES_READ)
                    ? "/admin/alert-types" : "/notifications";
            ctx.addCitation("referential", null, "Types d'alertes", href);
            return ctx.toJson(Map.of("count", items.size(), "items", items, "uiPath", href));
        });
    }

    @Tool(name = "list_alert_rules", description = """
            Règles d'alerte d'un template de circuit (UUID template).
            Permission ALERT_RULES:READ requise.
            """)
    public String listAlertRules(
            @ToolParam(description = "UUID du chain template") String chainTemplateId) {
        return ctx.runTool("list_alert_rules", Map.of("chainTemplateId", nz(chainTemplateId)), () -> {
            if (!ctx.hasPerm(RbacPermissions.ALERT_RULES_READ)) {
                return ctx.errorJson("PERMISSION_DENIED",
                        "Permission ALERT_RULES:READ requise pour lire les règles d'escalade.");
            }
            UUID id = parseUuid(chainTemplateId);
            if (id == null) {
                return ctx.errorJson("INVALID_ID", "chainTemplateId UUID invalide.");
            }
            List<AlertRule> rules = alertRuleService.listByTemplate(id);
            List<Map<String, Object>> items = new ArrayList<>();
            for (AlertRule rule : rules.stream().limit(MAX_LIST).toList()) {
                AlertRuleResponse r = AlertRuleMapper.toResponse(rule);
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", r.id());
                m.put("thresholdCode", r.thresholdCode());
                m.put("offsetValue", r.offsetValue());
                m.put("offsetUnit", r.offsetUnit());
                m.put("alertTypeCode", r.alertTypeCode());
                m.put("escalationLevel", r.escalationLevel());
                m.put("targetMode", r.targetMode());
                m.put("targetRole", r.targetRole());
                m.put("chainStepTemplateLabel", r.chainStepTemplateLabel());
                m.put("active", r.active());
                items.add(m);
            }
            ctx.addCitation("referential", id, "Règles d'alerte",
                    "/admin/chain-templates/" + id);
            return ctx.toJson(Map.of(
                    "chainTemplateId", id,
                    "count", rules.size(),
                    "items", items,
                    "uiPath", "/admin/chain-templates/" + id));
        });
    }

    @Tool(name = "list_business_calendar", description = """
            Jours fériés / calendrier ouvrable. Filtres year (ex. 2026), countryCode (défaut CM).
            Permission BUSINESS_CALENDAR:READ requise.
            """)
    public String listBusinessCalendar(
            @ToolParam(required = false, description = "Année (ex. 2026)") Integer year,
            @ToolParam(required = false, description = "Code pays (CM)") String countryCode) {
        Map<String, Object> args = new LinkedHashMap<>();
        args.put("year", year);
        args.put("countryCode", countryCode);
        return ctx.runTool("list_business_calendar", args, () -> {
            if (!ctx.hasPerm(RbacPermissions.BUSINESS_CALENDAR_READ)) {
                return ctx.errorJson("PERMISSION_DENIED",
                        "Permission BUSINESS_CALENDAR:READ requise. Écran admin : /admin/settings");
            }
            String country = blankToNull(countryCode) != null ? countryCode.trim() : "CM";
            List<BusinessCalendarDay> days = businessCalendarDayService.list(year, country);
            List<Map<String, Object>> items = new ArrayList<>();
            for (BusinessCalendarDay day : days.stream().limit(50).toList()) {
                BusinessCalendarDayResponse r = BusinessCalendarDayMapper.toResponse(day);
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("calendarDate", r.calendarDate());
                m.put("label", r.label());
                m.put("countryCode", r.countryCode());
                items.add(m);
            }
            ctx.addCitation("referential", null, "Calendrier", "/admin/settings");
            return ctx.toJson(Map.of(
                    "year", year,
                    "countryCode", country,
                    "count", days.size(),
                    "items", items,
                    "uiPath", "/admin/settings"));
        });
    }

    @Tool(name = "list_preconfigured_dossiers", description = """
            Dossiers préconfigurés (formulaire / portail). Permission FILE_TYPES:READ requise.
            """)
    public String listPreconfiguredDossiers() {
        return ctx.runTool("list_preconfigured_dossiers", Map.of(), () -> {
            if (!ctx.hasPerm(RbacPermissions.FILE_TYPES_READ)) {
                return ctx.errorJson("PERMISSION_DENIED",
                        "Permission FILE_TYPES:READ requise pour les dossiers préconfigurés.");
            }
            List<PreconfiguredDossier> all = preconfiguredDossierService.listAll();
            List<Map<String, Object>> items = new ArrayList<>();
            for (PreconfiguredDossier d : all.stream().limit(MAX_LIST).toList()) {
                PreconfiguredDossierResponse r = PreconfiguredDossierMapper.toResponse(d);
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", r.id());
                m.put("code", r.code());
                m.put("name", r.name());
                m.put("fileTypeCode", r.fileTypeCode());
                m.put("chainTemplateCode", r.chainTemplateCode());
                m.put("portalEnabled", r.portalEnabled());
                m.put("active", r.active());
                m.put("uiPath", "/admin/preconfigured-dossiers/" + r.id());
                items.add(m);
            }
            ctx.addCitation("referential", null, "Préconfigurés", "/admin/preconfigured-dossiers");
            return ctx.toJson(Map.of(
                    "count", all.size(),
                    "items", items,
                    "uiPath", "/admin/preconfigured-dossiers"));
        });
    }

    @Tool(name = "lookup_help", description = """
            Aide FluxPro : comment transmettre, créer, clôturer, lire le dashboard, permissions, etc.
            Toujours utiliser cet outil pour les questions « comment faire » / guide UI.
            query = question utilisateur en langage naturel.
            """)
    public String lookupHelp(
            @ToolParam(description = "Question ou mots-clés d'aide") String query) {
        return ctx.runTool("lookup_help", Map.of("query", nz(query)), () -> {
            Map<String, Object> result = new LinkedHashMap<>(helpKnowledgeBase.lookup(query, 3));
            result.put("uiHint", "Fournir les étapes et le deep-link uiPath sans exécuter d'action.");
            return ctx.toJson(result);
        });
    }

    private Map<String, Object> orgNode(OrganizationTreeResponse node) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", node.id());
        m.put("code", node.code());
        m.put("name", node.name());
        if (node.type() != null) {
            m.put("typeCode", node.type().code());
            m.put("typeName", node.type().name());
        }
        m.put("active", node.active());
        if (node.children() != null && !node.children().isEmpty()) {
            m.put("children", node.children().stream().map(this::orgNode).toList());
        }
        return m;
    }

    private OrganizationDetailResponse resolveOrganization(String organizationId, String code) {
        UUID id = parseUuid(organizationId);
        if (id != null) {
            return organizationService.getDetailById(id, ctx.actor());
        }
        if (blankToNull(code) != null) {
            return organizationService.getDetailByCode(code, ctx.actor());
        }
        return null;
    }

    private Map<String, Object> orgDetailMap(OrganizationDetailResponse org) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", org.id());
        payload.put("code", org.code());
        payload.put("name", org.name());
        payload.put("typeCode", org.typeCode());
        payload.put("typeName", org.typeName());
        payload.put("parentId", org.parentId());
        payload.put("parentCode", org.parentCode());
        payload.put("active", org.active());
        payload.put("uiPath", "/admin/org/" + org.id());
        return payload;
    }

    private List<String> buildHierarchyPath(OrganizationDetailResponse org) {
        List<String> path = new ArrayList<>();
        path.add(org.code());
        String parentCode = org.parentCode();
        int guard = 0;
        while (parentCode != null && !parentCode.isBlank() && guard++ < 12) {
            path.add(0, parentCode);
            try {
                OrganizationDetailResponse parent = organizationService.getDetailByCode(parentCode, ctx.actor());
                parentCode = parent.parentCode();
            } catch (Exception e) {
                break;
            }
        }
        return path;
    }

    private OrganizationTreeResponse findInTree(List<OrganizationTreeResponse> roots, UUID id) {
        if (roots == null || id == null) {
            return null;
        }
        for (OrganizationTreeResponse n : roots) {
            OrganizationTreeResponse found = findNode(n, id);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private OrganizationTreeResponse findNode(OrganizationTreeResponse node, UUID id) {
        if (id.equals(node.id())) {
            return node;
        }
        if (node.children() == null) {
            return null;
        }
        for (OrganizationTreeResponse child : node.children()) {
            OrganizationTreeResponse found = findNode(child, id);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private String usersPageJson(Page<UserResponse> page, String organizationCode, String uiPath) {
        return ctx.toJson(usersPagePayload(page, organizationCode, uiPath));
    }

    private Map<String, Object> usersPagePayload(Page<UserResponse> page, String organizationCode, String uiPath) {
        List<Map<String, Object>> items = new ArrayList<>();
        StringBuilder list = new StringBuilder();
        for (UserResponse u : page.getContent()) {
            items.add(userDetailMap(u));
            list.append(formatUserListLine(u)).append("\n");
        }
        ctx.addCitation("users", null, "Utilisateurs", uiPath);
        Map<String, Object> payload = new LinkedHashMap<>();
        if (organizationCode != null) {
            payload.put("organizationCode", organizationCode);
        }
        payload.put("totalElements", page.getTotalElements());
        payload.put("count", items.size());
        payload.put("items", items);
        payload.put("listMarkdown", list.toString().trim());
        payload.put("preferredFormat", "listMarkdown");
        payload.put("uiPath", uiPath);
        if (items.isEmpty()) {
            payload.put("hint",
                    "Aucun utilisateur trouvé dans votre périmètre. "
                            + "Vérifiez l'orthographe, réessayez avec le seul nom de famille, "
                            + "ou confirmez la permission USERS:READ / votre rôle.");
        }
        return payload;
    }

    private boolean canListUsers() {
        return accessControlService.canReadUsers(ctx.actor());
    }

    private Map<String, Object> userDetailMap(UserResponse u) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", u.id());
        m.put("staffNumber", u.staffNumber());
        m.put("email", u.email());
        m.put("firstName", u.firstName());
        m.put("lastName", u.lastName());
        m.put("displayName", displayName(u));
        m.put("phone", u.phone());
        m.put("role", u.role() != null ? u.role().name() : null);
        m.put("jobTitle", u.jobTitle());
        m.put("active", u.active());
        m.put("organizationHead", u.organizationHead());
        m.put("substituteId", u.substituteId());
        m.put("substituteDisplayName", u.substituteDisplayName());
        if (u.organization() != null) {
            m.put("organizationId", u.organization().id());
            m.put("organizationCode", u.organization().code());
            m.put("organizationName", u.organization().name());
        }
        if (u.roles() != null) {
            m.put("roles", u.roles().stream().map(RoleSummaryResponse::name).toList());
        }
        m.put("uiPath", "/admin/users/" + u.id());
        return m;
    }

    private static String formatUserListLine(UserResponse u) {
        StringBuilder sb = new StringBuilder();
        sb.append("- **").append(escapeMd(displayName(u))).append("**");
        if (u.staffNumber() != null && !u.staffNumber().isBlank()) {
            sb.append(" (").append(escapeMd(u.staffNumber())).append(")");
        }
        if (u.role() != null) {
            sb.append(" — ").append(u.role().name());
        }
        if (u.organization() != null && u.organization().code() != null) {
            sb.append(" · ").append(escapeMd(u.organization().code()));
        }
        if (u.organizationHead()) {
            sb.append(" · chef d'org");
        }
        if (!u.active()) {
            sb.append(" · inactif");
        }
        return sb.toString();
    }

    private static String displayName(UserResponse u) {
        return (nz(u.firstName()) + " " + nz(u.lastName())).trim();
    }

    private static UserRole parseRole(String role) {
        if (role == null || role.isBlank()) {
            return null;
        }
        try {
            return UserRole.valueOf(role.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static String escapeMd(String v) {
        if (v == null || v.isEmpty()) {
            return "";
        }
        return v.replace("|", "/").replace("\n", " ").trim();
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
}
