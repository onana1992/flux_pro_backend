package com.nanotech.flux_pro_backend.service.assistant;

import com.nanotech.flux_pro_backend.dto.response.OrganizationDetailResponse;
import com.nanotech.flux_pro_backend.dto.response.OrganizationSummaryResponse;
import com.nanotech.flux_pro_backend.dto.response.UserResponse;
import com.nanotech.flux_pro_backend.entity.Organization;
import com.nanotech.flux_pro_backend.entity.OrganizationType;
import com.nanotech.flux_pro_backend.entity.User;
import com.nanotech.flux_pro_backend.enumeration.UserRole;
import com.nanotech.flux_pro_backend.security.AccessControlService;
import com.nanotech.flux_pro_backend.security.OrganizationScopeService;
import com.nanotech.flux_pro_backend.security.RbacAuthorityService;
import com.nanotech.flux_pro_backend.security.RbacPermissions;
import com.nanotech.flux_pro_backend.security.SecurityUser;
import com.nanotech.flux_pro_backend.service.AlertRuleService;
import com.nanotech.flux_pro_backend.service.AlertTypeService;
import com.nanotech.flux_pro_backend.service.BusinessCalendarDayService;
import com.nanotech.flux_pro_backend.service.ChainTemplateService;
import com.nanotech.flux_pro_backend.service.FileTypeService;
import com.nanotech.flux_pro_backend.service.OrganizationService;
import com.nanotech.flux_pro_backend.service.PreconfiguredDossierService;
import com.nanotech.flux_pro_backend.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssistantCatalogToolsTest {

    @Mock private OrganizationService organizationService;
    @Mock private OrganizationScopeService organizationScopeService;
    @Mock private AccessControlService accessControlService;
    @Mock private UserService userService;
    @Mock private FileTypeService fileTypeService;
    @Mock private ChainTemplateService chainTemplateService;
    @Mock private AlertTypeService alertTypeService;
    @Mock private AlertRuleService alertRuleService;
    @Mock private BusinessCalendarDayService businessCalendarDayService;
    @Mock private PreconfiguredDossierService preconfiguredDossierService;
    @Mock private AssistantHelpKnowledgeBase helpKnowledgeBase;

    private AssistantCatalogTools agentTools;

    @BeforeEach
    void setUp() {
        agentTools = toolsFor(List.of());
    }

    @Test
    void agent_cannotListChainTemplates() {
        String json = agentTools.listChainTemplates(true, null, null);
        assertThat(json).contains("PERMISSION_DENIED").contains("CHAIN_TEMPLATES:READ");
        verify(chainTemplateService, never()).findAllSummaries(any(), any(), any(), any());
    }

    @Test
    void agent_cannotListBusinessCalendar() {
        String json = agentTools.listBusinessCalendar(2026, "CM");
        assertThat(json).contains("PERMISSION_DENIED").contains("BUSINESS_CALENDAR:READ");
        verify(businessCalendarDayService, never()).list(any(), any());
    }

    @Test
    void lookupHelp_availableForAgent() {
        when(helpKnowledgeBase.lookup("comment transmettre", 3)).thenReturn(Map.of(
                "query", "comment transmettre",
                "hits", List.of(Map.of("sectionId", "transmettre-dossier", "content", "Étapes…"))));

        String json = agentTools.lookupHelp("comment transmettre");
        assertThat(json).contains("transmettre-dossier");
    }

    @Test
    void listOrganizationUsers_withDescendants_usesOrgIdSet() {
        AssistantCatalogTools tools = toolsFor(List.of(RbacPermissions.USERS_READ));
        UUID dagId = UUID.randomUUID();
        UUID childId = UUID.randomUUID();
        OrganizationDetailResponse dag = new OrganizationDetailResponse(
                dagId, "DAG", "Direction des Affaires Générales",
                null, "DIR", "Direction", null, null, true);
        when(accessControlService.canReadUsers(any())).thenReturn(true);
        when(organizationService.getDetailByCode(eq("DAG"), any())).thenReturn(dag);
        when(organizationScopeService.collectSelfAndDescendants(dagId))
                .thenReturn(Set.of(dagId, childId));

        UserResponse user = new UserResponse(
                UUID.randomUUID(), "MAT-9", "a@mintp.cm", "Nguema", "Paul", null,
                UserRole.AGENT,
                new OrganizationSummaryResponse(childId, "DAG-RH", "RH"),
                null, true, false, null, null, false, List.of());
        when(userService.search(any(), isNull(), eq(Set.of(dagId, childId)), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(user)));

        String json = tools.listOrganizationUsers(null, "DAG", null, true);

        assertThat(json).contains("includeDescendants").contains("true")
                .contains("DAG-RH").contains("Paul");
        verify(userService).search(any(), isNull(), eq(Set.of(dagId, childId)), isNull(), isNull(), any(Pageable.class));
        verify(userService, never()).search(any(), eq(dagId), any(), any(), any(Pageable.class));
    }

    @Test
    void listOrganizationUsers_deniedWithoutReadUsers() {
        when(accessControlService.canReadUsers(any())).thenReturn(false);
        String json = agentTools.listOrganizationUsers(null, "DAG", null, true);
        assertThat(json).contains("PERMISSION_DENIED");
        verify(userService, never()).search(any(), any(), any(), any(), any(), any());
    }

    private AssistantCatalogTools toolsFor(List<String> permissions) {
        AssistantToolContext ctx = new AssistantToolContext(securityUser(permissions), 5);
        return new AssistantCatalogTools(
                ctx,
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
    }

    private static SecurityUser securityUser(List<String> permissions) {
        OrganizationType type = new OrganizationType();
        type.setId(UUID.randomUUID());
        type.setCode("DIR");
        type.setName("Direction");
        type.setActive(true);

        Organization org = new Organization();
        org.setId(UUID.randomUUID());
        org.setCode("DAG");
        org.setName("DAG");
        org.setOrganizationType(type);
        org.setActive(true);

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("agent@mintp.cm");
        user.setPasswordHash("hash");
        user.setRole(UserRole.AGENT);
        user.setOrganization(org);
        user.setStaffNumber("MAT-1");
        user.setLastName("Dupont");
        user.setFirstName("Ada");
        user.setActive(true);
        user.setMustChangePassword(false);

        return new SecurityUser(
                user,
                new RbacAuthorityService.RbacAuthorities(List.of("AGENT"), permissions));
    }
}
