package com.nanotech.flux_pro_backend.service;

import com.nanotech.flux_pro_backend.common.DashboardException;
import com.nanotech.flux_pro_backend.dto.response.DashboardSummaryResponse;
import com.nanotech.flux_pro_backend.dto.response.OrganizationRankingResponse;
import com.nanotech.flux_pro_backend.dto.response.WorkloadEntryResponse;
import com.nanotech.flux_pro_backend.entity.ChainStepTemplate;
import com.nanotech.flux_pro_backend.entity.ChainTemplate;
import com.nanotech.flux_pro_backend.entity.FileEntity;
import com.nanotech.flux_pro_backend.entity.FilePassage;
import com.nanotech.flux_pro_backend.entity.Organization;
import com.nanotech.flux_pro_backend.entity.OrganizationType;
import com.nanotech.flux_pro_backend.entity.User;
import com.nanotech.flux_pro_backend.enumeration.DelayUnit;
import com.nanotech.flux_pro_backend.enumeration.FileStatus;
import com.nanotech.flux_pro_backend.enumeration.UserRole;
import com.nanotech.flux_pro_backend.repository.FilePassageRepository;
import com.nanotech.flux_pro_backend.repository.FileRepository;
import com.nanotech.flux_pro_backend.repository.FileTypeRepository;
import com.nanotech.flux_pro_backend.repository.OrganizationTypeRepository;
import com.nanotech.flux_pro_backend.security.AccessControlService;
import com.nanotech.flux_pro_backend.security.OrganizationScopeService;
import com.nanotech.flux_pro_backend.security.SecurityUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private OrganizationScopeService organizationScopeService;
    @Mock
    private AccessControlService accessControlService;
    @Mock
    private OrganizationTypeRepository organizationTypeRepository;
    @Mock
    private FileRepository fileRepository;
    @Mock
    private FilePassageRepository filePassageRepository;
    @Mock
    private FileTypeRepository fileTypeRepository;
    @Mock
    private DelaiService delaiService;
    @Mock
    private SecurityUser actor;

    @InjectMocks
    private DashboardService dashboardService;

    @BeforeEach
    void setUp() {
        lenient().when(actor.getId()).thenReturn(UUID.randomUUID());
        lenient().when(actor.getRole()).thenReturn(UserRole.DIRECTOR);
        lenient().when(actor.getOrganizationCode()).thenReturn("DIER");
        lenient().when(actor.getOrganizationId()).thenReturn(UUID.randomUUID());
    }

    @Test
    void summary_returnsZeroCounts_whenScopeIsEmpty() {
        when(organizationScopeService.resolveScopeFilter(actor))
                .thenReturn(new OrganizationScopeService.ScopeFilter(false, Set.of()));

        DashboardSummaryResponse response = dashboardService.summary(actor, null, null);

        assertThat(response.activeFiles()).isZero();
        assertThat(response.overdueFiles()).isZero();
        assertThat(response.closedThisMonth()).isZero();
        assertThat(response.createdThisMonth()).isZero();
    }

    @Test
    void summary_delegatesToRepositories_withGlobalScope() {
        when(organizationScopeService.resolveScopeFilter(actor))
                .thenReturn(new OrganizationScopeService.ScopeFilter(true, Set.of()));
        when(fileRepository.countActiveByScope(eq(true), anySet(), isNull(), isNull())).thenReturn(47L);
        when(fileRepository.countClosedBetween(any(), any(), eq(true), anySet(), isNull(), isNull())).thenReturn(12L);
        when(fileRepository.countReceivedBetween(any(), any(), eq(true), anySet(), isNull(), isNull())).thenReturn(15L);
        when(filePassageRepository.findOverdueForScope(any(), eq(true), anySet(), isNull())).thenReturn(List.of());

        DashboardSummaryResponse response = dashboardService.summary(actor, null, null);

        assertThat(response.activeFiles()).isEqualTo(47L);
        assertThat(response.closedThisMonth()).isEqualTo(12L);
        assertThat(response.createdThisMonth()).isEqualTo(15L);
    }

    @Test
    void myActivity_countsOverdueAmongActivePassages() {
        Instant now = Instant.now();
        FilePassage onTime = buildPassage(now.plus(2, ChronoUnit.DAYS));
        FilePassage overdue = buildPassage(now.minus(1, ChronoUnit.DAYS));
        when(filePassageRepository.findActiveByResponsibleUser(actor.getId()))
                .thenReturn(List.of(onTime, overdue));
        when(filePassageRepository.findRecentTransmissionsByResponsibleUser(any(), any()))
                .thenReturn(List.of());
        lenient().when(delaiService.isOverdue(eq(onTime.getDueAt()), any())).thenReturn(false);
        lenient().when(delaiService.isOverdue(eq(overdue.getDueAt()), any())).thenReturn(true);

        var response = dashboardService.myActivity(actor);

        assertThat(response.activeCount()).isEqualTo(2);
        assertThat(response.overdueCount()).isEqualTo(1);
        assertThat(response.items()).hasSize(2);
    }

    @Test
    void workload_sortsByOverdueCountDescending() {
        when(organizationScopeService.resolveScopeFilter(actor))
                .thenReturn(new OrganizationScopeService.ScopeFilter(true, Set.of()));

        Organization org = buildOrganization("DAG", null);
        User agentA = buildUser("Alice", "A", org);
        User agentB = buildUser("Bob", "B", org);

        Instant now = Instant.now();
        FilePassage aActive = buildPassageForUser(agentA, now.plus(5, ChronoUnit.DAYS));
        FilePassage bOverdue1 = buildPassageForUser(agentB, now.minus(1, ChronoUnit.DAYS));
        FilePassage bOverdue2 = buildPassageForUser(agentB, now.minus(2, ChronoUnit.DAYS));

        when(filePassageRepository.findActiveForWorkload(eq(true), anySet(), isNull()))
                .thenReturn(List.of(aActive, bOverdue1, bOverdue2));
        lenient().when(delaiService.isOverdue(eq(aActive.getDueAt()), any())).thenReturn(false);
        lenient().when(delaiService.isOverdue(eq(bOverdue1.getDueAt()), any())).thenReturn(true);
        lenient().when(delaiService.isOverdue(eq(bOverdue2.getDueAt()), any())).thenReturn(true);

        List<WorkloadEntryResponse> result = dashboardService.workload(actor, null);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).firstName()).isEqualTo("Bob");
        assertThat(result.get(0).overdueCount()).isEqualTo(2);
        assertThat(result.get(1).firstName()).isEqualTo("Alice");
        assertThat(result.get(1).overdueCount()).isZero();
    }

    @Test
    void complianceRanking_rejectsUnknownGroupType() {
        when(organizationTypeRepository.existsByCode("UNKNOWN")).thenReturn(false);

        assertThatThrownBy(() -> dashboardService.complianceRanking(actor, "UNKNOWN", 90))
                .isInstanceOf(DashboardException.class)
                .hasMessageContaining("UNKNOWN");
    }

    @Test
    void complianceRanking_rejectsWindowDaysOutOfBounds() {
        assertThatThrownBy(() -> dashboardService.complianceRanking(actor, "DIRECTORATE", 0))
                .isInstanceOf(DashboardException.class)
                .hasMessageContaining("windowDays");
    }

    @Test
    void complianceRanking_groupsByAncestorAndComputesRate() {
        when(organizationTypeRepository.existsByCode("DIRECTORATE")).thenReturn(true);
        when(organizationScopeService.resolveScopeFilter(actor))
                .thenReturn(new OrganizationScopeService.ScopeFilter(true, Set.of()));

        OrganizationType directorateType = buildOrganizationType("DIRECTORATE");
        Organization directorate = buildOrganization("DIER", directorateType);
        Organization service = buildOrganization("DIER-SVC", null);
        service.setParent(directorate);

        ChainTemplate template = new ChainTemplate();
        template.setId(UUID.randomUUID());
        template.setTotalDelayDays(10);
        template.setDelayUnit(DelayUnit.WORKING_DAYS);

        Instant compliantClosedAt = Instant.now();
        Instant lateClosedAt = compliantClosedAt.plusSeconds(3600);
        FileEntity compliant = buildClosedFile(service, template, compliantClosedAt);
        FileEntity late = buildClosedFile(service, template, lateClosedAt);

        when(fileRepository.findClosedSince(any(), eq(true), anySet(), isNull()))
                .thenReturn(List.of(compliant, late));
        lenient().when(delaiService.countWorkingDays(any(), eq(compliantClosedAt))).thenReturn(5);
        lenient().when(delaiService.countWorkingDays(any(), eq(lateClosedAt))).thenReturn(20);

        List<OrganizationRankingResponse> ranking = dashboardService.complianceRanking(actor, "DIRECTORATE", 90);

        assertThat(ranking).hasSize(1);
        OrganizationRankingResponse entry = ranking.get(0);
        assertThat(entry.organizationCode()).isEqualTo("DIER");
        assertThat(entry.closedCount()).isEqualTo(2);
        assertThat(entry.compliantCount()).isEqualTo(1);
        assertThat(entry.complianceRate()).isEqualTo(50.0);
    }

    private FilePassage buildPassage(Instant dueAt) {
        FileEntity file = new FileEntity();
        file.setId(UUID.randomUUID());
        file.setReferenceNumber("MINTP-DAG-2026-0001");
        file.setSubject("Test");
        file.setStatus(FileStatus.IN_PROGRESS);

        ChainStepTemplate step = new ChainStepTemplate();
        step.setId(UUID.randomUUID());
        step.setLabel("Instruction");

        FilePassage passage = new FilePassage();
        passage.setId(UUID.randomUUID());
        passage.setFile(file);
        passage.setChainStepTemplate(step);
        passage.setStatus(com.nanotech.flux_pro_backend.enumeration.PassageStatus.IN_PROGRESS);
        passage.setDueAt(dueAt);
        passage.setReceivedAt(Instant.now().minus(3, ChronoUnit.DAYS));
        return passage;
    }

    private FilePassage buildPassageForUser(User user, Instant dueAt) {
        FilePassage passage = buildPassage(dueAt);
        passage.setResponsibleUser(user);
        return passage;
    }

    private User buildUser(String firstName, String lastName, Organization org) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setOrganization(org);
        return user;
    }

    private Organization buildOrganization(String code, OrganizationType type) {
        Organization org = new Organization();
        org.setId(UUID.randomUUID());
        org.setCode(code);
        org.setName(code);
        org.setOrganizationType(type);
        return org;
    }

    private OrganizationType buildOrganizationType(String code) {
        OrganizationType type = new OrganizationType();
        type.setId(UUID.randomUUID());
        type.setCode(code);
        type.setName(code);
        return type;
    }

    private FileEntity buildClosedFile(Organization org, ChainTemplate template, Instant closedAt) {
        FileEntity file = new FileEntity();
        file.setId(UUID.randomUUID());
        file.setOrganization(org);
        file.setChainTemplate(template);
        file.setStatus(FileStatus.CLOSED);
        file.setReceivedAt(java.time.LocalDate.now().minusDays(10));
        file.setClosedAt(closedAt);
        return file;
    }
}
