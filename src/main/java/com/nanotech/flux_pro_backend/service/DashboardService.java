package com.nanotech.flux_pro_backend.service;

import com.nanotech.flux_pro_backend.common.DashboardException;
import com.nanotech.flux_pro_backend.dto.response.DashboardSummaryResponse;
import com.nanotech.flux_pro_backend.dto.response.DelayByTypeResponse;
import com.nanotech.flux_pro_backend.dto.response.MyActivityItemResponse;
import com.nanotech.flux_pro_backend.dto.response.MyActivityResponse;
import com.nanotech.flux_pro_backend.dto.response.OrganizationRankingResponse;
import com.nanotech.flux_pro_backend.dto.response.OverdueFileResponse;
import com.nanotech.flux_pro_backend.dto.response.WorkloadEntryResponse;
import com.nanotech.flux_pro_backend.entity.ChainTemplate;
import com.nanotech.flux_pro_backend.entity.FileEntity;
import com.nanotech.flux_pro_backend.entity.FilePassage;
import com.nanotech.flux_pro_backend.entity.FileType;
import com.nanotech.flux_pro_backend.entity.Organization;
import com.nanotech.flux_pro_backend.entity.User;
import com.nanotech.flux_pro_backend.enumeration.DashboardScopeWidth;
import com.nanotech.flux_pro_backend.enumeration.DelayUnit;
import com.nanotech.flux_pro_backend.enumeration.UserRole;
import com.nanotech.flux_pro_backend.repository.FileRepository;
import com.nanotech.flux_pro_backend.repository.FileTypeRepository;
import com.nanotech.flux_pro_backend.repository.FilePassageRepository;
import com.nanotech.flux_pro_backend.repository.OrganizationTypeRepository;
import com.nanotech.flux_pro_backend.security.AccessControlService;
import com.nanotech.flux_pro_backend.security.OrganizationScopeService;
import com.nanotech.flux_pro_backend.security.SecurityUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Orchestration des widgets du module DSH (cf. SPEC-DSH.md). Ne réimplémente jamais la résolution
 * du périmètre organisationnel (déléguée à {@link OrganizationScopeService}) ni le calcul des
 * délais (délégué à {@link DelaiService}) ni la définition du retard (réutilisée depuis ALR).
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    /** Approximation documentée (SPEC-DSH.md §6.5) : journée ouvrée MINTP = 8h-17h = 9h. */
    private static final double WORKING_HOURS_PER_DAY = 9.0;
    private static final int MAX_WINDOW_DAYS = 365;

    private final OrganizationScopeService organizationScopeService;
    private final AccessControlService accessControlService;
    private final OrganizationTypeRepository organizationTypeRepository;
    private final FileRepository fileRepository;
    private final FilePassageRepository filePassageRepository;
    private final FileTypeRepository fileTypeRepository;
    private final DelaiService delaiService;
    private final ClockService clockService;

    @Transactional(readOnly = true)
    public DashboardSummaryResponse summary(SecurityUser actor, UUID organizationId, String fileTypeCode) {
        ResolvedScope scope = resolveScope(actor, organizationId);
        String organizationCode = resolveOrganizationCode(actor, organizationId);
        if (scope.empty()) {
            return new DashboardSummaryResponse(organizationId, organizationCode, scope.width(), 0, 0, 0, 0);
        }

        Instant now = clockService.now();
        long active = fileRepository.countActiveByScope(scope.allOrgs(), scope.orgIds(), organizationId, fileTypeCode);
        long overdue = filePassageRepository
                .findOverdueForScope(now, scope.allOrgs(), scope.orgIds(), organizationId)
                .stream()
                .map(p -> p.getFile().getId())
                .distinct()
                .count();

        ZonedDateTime monthStart = now.atZone(delaiService.zoneId())
                .toLocalDate().withDayOfMonth(1).atStartOfDay(delaiService.zoneId());
        long closedThisMonth = fileRepository.countClosedBetween(
                monthStart.toInstant(), now, scope.allOrgs(), scope.orgIds(), organizationId, fileTypeCode);
        long createdThisMonth = fileRepository.countReceivedBetween(
                monthStart.toLocalDate(), now.atZone(delaiService.zoneId()).toLocalDate(),
                scope.allOrgs(), scope.orgIds(), organizationId, fileTypeCode);

        return new DashboardSummaryResponse(
                organizationId, organizationCode, scope.width(), active, overdue, closedThisMonth, createdThisMonth);
    }

    /** DSH-01 — jamais filtré par périmètre organisationnel : chacun voit sa propre activité. */
    @Transactional(readOnly = true)
    public MyActivityResponse myActivity(SecurityUser actor) {
        Instant now = clockService.now();
        List<FilePassage> active = filePassageRepository.findActiveByResponsibleUser(actor.getId());
        Instant since = now.minusSeconds(30L * 24 * 3600);
        List<FilePassage> transmitted = filePassageRepository
                .findRecentTransmissionsByResponsibleUser(actor.getId(), since);

        List<MyActivityItemResponse> items = active.stream()
                .sorted(Comparator.comparing(FilePassage::getDueAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(p -> toActivityItem(p, now))
                .toList();
        long overdueCount = items.stream().filter(MyActivityItemResponse::overdue).count();

        return new MyActivityResponse(active.size(), overdueCount, transmitted.size(), items);
    }

    /** DSH-02 — charge par agent, triée par retards décroissants. */
    @Transactional(readOnly = true)
    public List<WorkloadEntryResponse> workload(SecurityUser actor, UUID organizationId) {
        ResolvedScope scope = resolveScope(actor, organizationId);
        if (scope.empty()) {
            return List.of();
        }
        Instant now = clockService.now();
        List<FilePassage> passages = filePassageRepository
                .findActiveForWorkload(scope.allOrgs(), scope.orgIds(), organizationId);

        Map<UUID, User> usersById = new HashMap<>();
        Map<UUID, Long> activeById = new HashMap<>();
        Map<UUID, Long> overdueById = new HashMap<>();
        for (FilePassage p : passages) {
            User u = p.getResponsibleUser();
            usersById.putIfAbsent(u.getId(), u);
            activeById.merge(u.getId(), 1L, Long::sum);
            if (p.getDueAt() != null && delaiService.isOverdue(p.getDueAt(), now)) {
                overdueById.merge(u.getId(), 1L, Long::sum);
            }
        }

        return usersById.values().stream()
                .map(u -> new WorkloadEntryResponse(
                        u.getId(),
                        u.getFirstName(),
                        u.getLastName(),
                        u.getOrganization() != null ? u.getOrganization().getCode() : null,
                        activeById.getOrDefault(u.getId(), 0L),
                        overdueById.getOrDefault(u.getId(), 0L)))
                .sorted(Comparator.comparingLong(WorkloadEntryResponse::overdueCount).reversed()
                        .thenComparing(Comparator.comparingLong(WorkloadEntryResponse::activeCount).reversed()))
                .toList();
    }

    /** DSH-01/02/03 — top des dossiers les plus en retard, même population que le digest ALR-08. */
    @Transactional(readOnly = true)
    public List<OverdueFileResponse> overdueFiles(SecurityUser actor, UUID organizationId, int limit) {
        ResolvedScope scope = resolveScope(actor, organizationId);
        if (scope.empty()) {
            return List.of();
        }
        int effectiveLimit = Math.max(1, Math.min(limit, 100));
        Instant now = clockService.now();
        return filePassageRepository.findOverdueForScope(now, scope.allOrgs(), scope.orgIds(), organizationId)
                .stream()
                .limit(effectiveLimit)
                .map(p -> toOverdueResponse(p, now))
                .toList();
    }

    /** DSH-06 — délai moyen réel par type de dossier sur la fenêtre glissante demandée. */
    @Transactional(readOnly = true)
    public List<DelayByTypeResponse> delayByType(SecurityUser actor, UUID organizationId, int windowDays) {
        validateWindowDays(windowDays);
        ResolvedScope scope = resolveScope(actor, organizationId);
        if (scope.empty()) {
            return List.of();
        }
        Instant from = clockService.now().minusSeconds((long) windowDays * 24 * 3600);
        List<FileEntity> closed = fileRepository.findClosedSince(
                from, scope.allOrgs(), scope.orgIds(), organizationId);

        Map<String, List<FileEntity>> byType = closed.stream()
                .filter(f -> f.getFileTypeCode() != null)
                .collect(Collectors.groupingBy(FileEntity::getFileTypeCode));

        List<DelayByTypeResponse> results = new ArrayList<>();
        for (Map.Entry<String, List<FileEntity>> entry : byType.entrySet()) {
            String typeCode = entry.getKey();
            List<FileEntity> files = entry.getValue();
            double averageDelay = files.stream()
                    .mapToInt(this::actualDelayDays)
                    .average()
                    .orElse(0);
            Integer targetDelay = resolveConsensusTargetDays(files);
            String label = fileTypeRepository.findByCodeIgnoreCase(typeCode).map(FileType::getName).orElse(typeCode);
            results.add(new DelayByTypeResponse(
                    typeCode, label, files.size(), Math.round(averageDelay * 100.0) / 100.0, targetDelay));
        }
        return results.stream()
                .sorted(Comparator.comparing(DelayByTypeResponse::fileTypeCode))
                .toList();
    }

    /** DSH-07 (et heatmap DSH-04) — taux de respect des délais, regroupé par un niveau d'{@code OrganizationType}. */
    @Transactional(readOnly = true)
    public List<OrganizationRankingResponse> complianceRanking(SecurityUser actor, String groupByTypeCode, int windowDays) {
        validateWindowDays(windowDays);
        if (!organizationTypeRepository.existsByCode(groupByTypeCode)) {
            throw DashboardException.badRequest(
                    "DASHBOARD_GROUP_TYPE_INVALID", "Type d'organisation inconnu : " + groupByTypeCode,
                    groupByTypeCode);
        }
        OrganizationScopeService.ScopeFilter scope = organizationScopeService.resolveScopeFilter(actor);
        if (!scope.allOrganizations() && scope.organizationIds().isEmpty()) {
            return List.of();
        }
        Set<UUID> orgIds = scope.organizationIds().isEmpty() ? Set.of(UUID.randomUUID()) : scope.organizationIds();
        Instant from = clockService.now().minusSeconds((long) windowDays * 24 * 3600);
        List<FileEntity> closed = fileRepository.findClosedSince(from, scope.allOrganizations(), orgIds, null);

        Map<Organization, List<FileEntity>> byGroup = new HashMap<>();
        for (FileEntity file : closed) {
            Organization group = resolveGroupAncestor(file.getOrganization(), groupByTypeCode);
            if (group != null) {
                byGroup.computeIfAbsent(group, g -> new ArrayList<>()).add(file);
            }
        }

        return byGroup.entrySet().stream()
                .map(e -> {
                    long closedCount = e.getValue().size();
                    long compliant = e.getValue().stream().filter(this::isCompliant).count();
                    double rate = closedCount == 0 ? 0 : Math.round(compliant * 10000.0 / closedCount) / 100.0;
                    Organization org = e.getKey();
                    return new OrganizationRankingResponse(
                            org.getId(), org.getCode(), org.getName(), closedCount, compliant, rate);
                })
                .sorted(Comparator.comparingDouble(OrganizationRankingResponse::complianceRate).reversed())
                .toList();
    }

    private MyActivityItemResponse toActivityItem(FilePassage p, Instant now) {
        boolean overdue = p.getDueAt() != null && delaiService.isOverdue(p.getDueAt(), now);
        return new MyActivityItemResponse(
                p.getId(),
                p.getFile().getId(),
                p.getFile().getReferenceNumber(),
                p.getFile().getSubject(),
                p.getChainStepTemplate().getLabel(),
                p.getReceivedAt(),
                p.getDueAt(),
                overdue);
    }

    private OverdueFileResponse toOverdueResponse(FilePassage p, Instant now) {
        int daysOverdue = delaiService.countWorkingDays(p.getDueAt(), now);
        String responsibleName = p.getResponsibleUser() != null
                ? p.getResponsibleUser().getFirstName() + " " + p.getResponsibleUser().getLastName()
                : null;
        return new OverdueFileResponse(
                p.getFile().getId(),
                p.getFile().getReferenceNumber(),
                p.getFile().getSubject(),
                p.getFile().getFileTypeCode(),
                p.getFile().getOrganization().getCode(),
                p.getChainStepTemplate().getLabel(),
                responsibleName,
                p.getDueAt(),
                daysOverdue);
    }

    private int actualDelayDays(FileEntity file) {
        Instant received = file.getReceivedAt().atStartOfDay(delaiService.zoneId()).toInstant();
        return delaiService.countWorkingDays(received, file.getClosedAt());
    }

    private boolean isCompliant(FileEntity file) {
        if (file.getChainTemplate() == null || file.getClosedAt() == null || file.getReceivedAt() == null) {
            return false;
        }
        return actualDelayDays(file) <= targetDelayDays(file.getChainTemplate());
    }

    private int targetDelayDays(ChainTemplate template) {
        if (template.getDelayUnit() == DelayUnit.WORKING_HOURS) {
            return (int) Math.ceil(template.getTotalDelayDays() / WORKING_HOURS_PER_DAY);
        }
        return template.getTotalDelayDays();
    }

    /** Retourne la cible commune si tous les dossiers du groupe partagent le même template, sinon {@code null}. */
    private Integer resolveConsensusTargetDays(List<FileEntity> files) {
        Set<UUID> templateIds = files.stream()
                .map(FileEntity::getChainTemplate)
                .filter(t -> t != null)
                .map(ChainTemplate::getId)
                .collect(Collectors.toSet());
        if (templateIds.size() != 1) {
            return null;
        }
        return files.stream()
                .map(FileEntity::getChainTemplate)
                .filter(t -> t != null)
                .findFirst()
                .map(this::targetDelayDays)
                .orElse(null);
    }

    /** Remonte l'arbre organisationnel jusqu'au premier ancêtre (inclus) dont le type correspond. */
    private Organization resolveGroupAncestor(Organization org, String groupByTypeCode) {
        Organization current = org;
        while (current != null) {
            if (current.getOrganizationType() != null
                    && groupByTypeCode.equalsIgnoreCase(current.getOrganizationType().getCode())) {
                return current;
            }
            current = current.getParent();
        }
        return null;
    }

    private void validateWindowDays(int windowDays) {
        if (windowDays < 1 || windowDays > MAX_WINDOW_DAYS) {
            throw new DashboardException(HttpStatus.BAD_REQUEST, "DASHBOARD_WINDOW_INVALID",
                    "windowDays doit être compris entre 1 et " + MAX_WINDOW_DAYS);
        }
    }

    private String resolveOrganizationCode(SecurityUser actor, UUID organizationId) {
        if (organizationId == null) {
            return actor.getOrganizationCode();
        }
        if (organizationId.equals(actor.getOrganizationId())) {
            return actor.getOrganizationCode();
        }
        return null;
    }

    private ResolvedScope resolveScope(SecurityUser actor, UUID organizationId) {
        OrganizationScopeService.ScopeFilter scope = organizationScopeService.resolveScopeFilter(actor);
        DashboardScopeWidth width = computeScopeWidth(actor, scope);
        if (organizationId != null) {
            accessControlService.assertCanAccessOrganization(actor, organizationId);
        }
        if (!scope.allOrganizations() && scope.organizationIds().isEmpty()) {
            return new ResolvedScope(false, Set.of(), width, true);
        }
        Set<UUID> orgIds = scope.organizationIds().isEmpty() ? Set.of(UUID.randomUUID()) : scope.organizationIds();
        return new ResolvedScope(scope.allOrganizations(), orgIds, width, false);
    }

    private DashboardScopeWidth computeScopeWidth(SecurityUser actor, OrganizationScopeService.ScopeFilter scope) {
        if (scope.allOrganizations()) {
            return DashboardScopeWidth.GLOBAL;
        }
        if (actor.getRole() == UserRole.REGIONAL_DIRECTOR) {
            return DashboardScopeWidth.REGIONAL;
        }
        return scope.organizationIds().size() > 1 ? DashboardScopeWidth.SUBTREE : DashboardScopeWidth.SELF;
    }

    private record ResolvedScope(boolean allOrgs, Set<UUID> orgIds, DashboardScopeWidth width, boolean empty) {
    }
}
