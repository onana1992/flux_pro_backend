package com.nanotech.flux_pro_backend.service;

import com.nanotech.flux_pro_backend.common.FileException;
import com.nanotech.flux_pro_backend.dto.request.ChainInitializeRequest;
import com.nanotech.flux_pro_backend.dto.request.ChainStepAssignmentRequest;
import com.nanotech.flux_pro_backend.dto.request.PassageCommentRequest;
import com.nanotech.flux_pro_backend.dto.request.PassageReasonRequest;
import com.nanotech.flux_pro_backend.dto.request.PassageReassignRequest;
import com.nanotech.flux_pro_backend.dto.request.PassageReturnRequest;
import com.nanotech.flux_pro_backend.dto.request.PassageTransmitRequest;
import com.nanotech.flux_pro_backend.dto.response.CurrentHolderResponse;
import com.nanotech.flux_pro_backend.dto.response.FilePassageCircuitResponse;
import com.nanotech.flux_pro_backend.dto.response.PassageCandidateResponse;
import com.nanotech.flux_pro_backend.entity.ChainStepTemplate;
import com.nanotech.flux_pro_backend.entity.ChainTemplate;
import com.nanotech.flux_pro_backend.entity.FileEntity;
import com.nanotech.flux_pro_backend.entity.FilePassage;
import com.nanotech.flux_pro_backend.entity.Organization;
import com.nanotech.flux_pro_backend.entity.User;
import com.nanotech.flux_pro_backend.enumeration.FileStatus;
import com.nanotech.flux_pro_backend.enumeration.PassageStatus;
import com.nanotech.flux_pro_backend.enumeration.UserRole;
import com.nanotech.flux_pro_backend.mapper.PassageMapper;
import com.nanotech.flux_pro_backend.repository.ChainTemplateRepository;
import com.nanotech.flux_pro_backend.repository.FilePassageRepository;
import com.nanotech.flux_pro_backend.repository.FileRepository;
import com.nanotech.flux_pro_backend.repository.OrganizationRepository;
import com.nanotech.flux_pro_backend.repository.UserRepository;
import com.nanotech.flux_pro_backend.security.AccessControlService;
import com.nanotech.flux_pro_backend.security.SecurityUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PassageService {

    private final FileRepository fileRepository;
    private final FilePassageRepository filePassageRepository;
    private final ChainTemplateRepository chainTemplateRepository;
    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final DelaiService delaiService;
    private final PassageAuthorityService passageAuthorityService;
    private final AccessControlService accessControlService;

    @Transactional
    public FilePassageCircuitResponse initializeChainForFile(
            UUID fileId, ChainInitializeRequest request, SecurityUser actor) {
        FileEntity file = loadFile(fileId, actor);
        if (file.getStatus() != FileStatus.IN_PROGRESS && file.getStatus() != FileStatus.ON_HOLD) {
            throw FileException.conflict(
                    "FILE_CHAIN_INIT_STATUS_INVALID", "Chain can only be initialized on active files");
        }
        if (filePassageRepository.existsByFileId(fileId)) {
            throw FileException.conflict("PASSAGE_CHAIN_EXISTS", "Chain already initialized for this file");
        }

        ChainTemplate template = chainTemplateRepository.findByIdWithSteps(request.chainTemplateId())
                .orElseThrow(() -> FileException.badRequest("PASSAGE_CHAIN_MISSING", "Chain template not found"));
        if (!template.isActive()) {
            throw FileException.badRequest("PASSAGE_CHAIN_INACTIVE", "Chain template is inactive");
        }

        List<ChainStepTemplate> steps = template.getSteps().stream()
                .sorted(Comparator.comparingInt(ChainStepTemplate::getStepOrder)
                        .thenComparing(ChainStepTemplate::getLabel, String.CASE_INSENSITIVE_ORDER))
                .toList();
        if (steps.isEmpty()) {
            throw FileException.badRequest("PASSAGE_CHAIN_EMPTY", "Chain template has no steps");
        }

        Map<UUID, UUID> assignments = toAssignmentMap(request.assignments());
        if (assignments.size() != steps.size()) {
            throw FileException.badRequest(
                    "PASSAGE_ASSIGNMENT_INCOMPLETE",
                    "A responsible user must be chosen for every step");
        }
        for (ChainStepTemplate step : steps) {
            if (!assignments.containsKey(step.getId())) {
                throw FileException.badRequest(
                        "PASSAGE_ASSIGNMENT_MISSING_FOR_STEP",
                        "Missing responsible user for step: " + step.getLabel(), step.getLabel());
            }
        }

        Map<UUID, User> usersById = loadResponsibleUsers(assignments.values());
        Set<UUID> ministryOrgIds = new LinkedHashSet<>(collectMinistryOrganizationIds(file.getOrganization()));
        for (ChainStepTemplate step : steps) {
            User responsible = usersById.get(assignments.get(step.getId()));
            if (!ministryOrgIds.contains(responsible.getOrganization().getId())) {
                throw FileException.badRequest(
                        "PASSAGE_USER_OUT_OF_SCOPE",
                        "Responsible user must belong to the same ministry tree as the file");
            }
        }

        file.setChainTemplate(template);
        fileRepository.save(file);
        initializeChain(file, steps, assignments, usersById);
        return getCircuit(fileId, actor);
    }

    @Transactional(readOnly = true)
    public List<PassageCandidateResponse> listCandidates(UUID fileId, UserRole role, SecurityUser actor) {
        FileEntity file = loadFile(fileId, actor);
        // Tout l'arbre du ministère (ex. MINTP + DSI + DAG + DRTP…), pas seulement les parents du dossier.
        List<UUID> orgIds = collectMinistryOrganizationIds(file.getOrganization());
        if (orgIds.isEmpty()) {
            return List.of();
        }
        return userRepository.findActiveByRoleInOrganizations(role, orgIds).stream()
                .map(u -> new PassageCandidateResponse(
                        u.getId(),
                        u.getFirstName(),
                        u.getLastName(),
                        u.getEmail(),
                        u.getRole(),
                        u.getOrganization().getCode(),
                        u.getOrganization().getName()))
                .toList();
    }

    @Transactional(readOnly = true)
    public FilePassageCircuitResponse getCircuit(UUID fileId, SecurityUser actor) {
        FileEntity file = loadFile(fileId, actor);
        List<FilePassage> passages = filePassageRepository.findByFileIdWithDetails(fileId);
        return PassageMapper.toCircuit(file, passages, delaiService);
    }

    @Transactional(readOnly = true)
    public CurrentHolderResponse getCurrentHolder(UUID fileId, SecurityUser actor) {
        FilePassage passage = loadCurrentPassage(fileId, actor);
        return PassageMapper.toCurrentHolder(passage, delaiService, Instant.now());
    }

    private void initializeChain(
            FileEntity file,
            List<ChainStepTemplate> steps,
            Map<UUID, UUID> assignments,
            Map<UUID, User> usersById) {
        Instant now = Instant.now();
        Integer firstStage = PassageStageHelper.distinctStagesFromSteps(steps).stream()
                .findFirst()
                .orElseThrow();
        for (ChainStepTemplate step : steps) {
            User responsible = usersById.get(assignments.get(step.getId()));
            FilePassage passage = new FilePassage();
            passage.setFile(file);
            passage.setChainStepTemplate(step);
            passage.setStepOrder(step.getStepOrder());
            passage.setResponsibleUser(responsible);
            if (step.getStepOrder() == firstStage) {
                passage.setStatus(PassageStatus.IN_PROGRESS);
                passage.setReceivedAt(now);
                passage.setDueAt(delaiService.calculateDueDate(now, step.getDelayValue(), step.getDelayUnit()));
            } else {
                passage.setStatus(PassageStatus.PENDING);
            }
            filePassageRepository.save(passage);
        }
    }

    @Transactional
    public FilePassageCircuitResponse transmit(
            UUID fileId, UUID passageId, PassageTransmitRequest request, SecurityUser actor) {
        FileEntity file = loadFile(fileId, actor);
        assertInProgress(file);
        FilePassage passage = loadPassage(fileId, passageId);
        assertActivePassage(passage);
        assertCanAct(passage, actor);

        Instant now = Instant.now();
        if (passage.getDueAt() != null
                && delaiService.isOverdue(passage.getDueAt(), now)
                && !StringUtils.hasText(request.comment())) {
            throw FileException.badRequest(
                    "PASSAGE_COMMENT_REQUIRED", "Comment is required when transmitting an overdue passage");
        }

        completePassage(passage, request.comment(), now);

        ChainStepTemplate currentStep = passage.getChainStepTemplate();
        List<FilePassage> allPassages = filePassageRepository.findByFileIdWithDetails(fileId);
        int currentStage = passage.getStepOrder();

        if (currentStep.isClosureStep()) {
            return PassageMapper.toCircuit(file, allPassages, delaiService);
        }

        if (!PassageStageHelper.isStageComplete(allPassages, currentStage)) {
            return PassageMapper.toCircuit(file, allPassages, delaiService);
        }

        Integer nextStage = PassageStageHelper.nextStage(allPassages, currentStage);
        if (nextStage == null) {
            return PassageMapper.toCircuit(file, allPassages, delaiService);
        }

        activateStage(file, allPassages, nextStage, request.nextResponsibleUserId(), now);
        return PassageMapper.toCircuit(file, filePassageRepository.findByFileIdWithDetails(fileId), delaiService);
    }

    @Transactional
    public FilePassageCircuitResponse returnToPrevious(
            UUID fileId, UUID passageId, PassageReturnRequest request, SecurityUser actor) {
        FileEntity file = loadFile(fileId, actor);
        assertInProgress(file);
        FilePassage passage = loadPassage(fileId, passageId);
        assertActivePassage(passage);
        assertCanAct(passage, actor);

        List<FilePassage> allPassages = filePassageRepository.findByFileIdWithDetails(fileId);
        int currentStage = passage.getStepOrder();
        if (currentStage <= 1) {
            throw FileException.badRequest("PASSAGE_RETURN_FORBIDDEN", "No previous passage");
        }
        int previousStage = currentStage - 1;

        Instant now = Instant.now();
        passage.setStatus(PassageStatus.RETURNED);
        passage.setReturnReason(request.reason().trim());
        passage.setTransmittedAt(now);
        filePassageRepository.save(passage);

        resetStageToPending(allPassages, currentStage);
        reactivateStage(allPassages, previousStage, now);

        return PassageMapper.toCircuit(file, filePassageRepository.findByFileIdWithDetails(fileId), delaiService);
    }

    @Transactional
    public FilePassageCircuitResponse suspend(
            UUID fileId, UUID passageId, PassageReasonRequest request, SecurityUser actor) {
        FileEntity file = loadFile(fileId, actor);
        assertInProgress(file);
        FilePassage passage = loadPassage(fileId, passageId);
        if (passage.getStatus() != PassageStatus.IN_PROGRESS) {
            throw FileException.conflict(
                    "PASSAGE_NOT_IN_PROGRESS_FOR_SUSPEND", "Only in-progress passages can be suspended");
        }
        assertCanAct(passage, actor);

        Instant now = Instant.now();
        passage.setStatus(PassageStatus.SUSPENDED);
        passage.setSuspendedAt(now);
        passage.setComment(request.reason().trim());
        filePassageRepository.save(passage);

        file.setStatus(FileStatus.ON_HOLD);
        file.setExternalHoldReason(request.reason().trim());
        file.setExternalHoldSince(now);
        fileRepository.save(file);

        return PassageMapper.toCircuit(file, filePassageRepository.findByFileIdWithDetails(fileId), delaiService);
    }

    @Transactional
    public FilePassageCircuitResponse resume(UUID fileId, UUID passageId, SecurityUser actor) {
        FileEntity file = loadFile(fileId, actor);
        FilePassage passage = loadPassage(fileId, passageId);
        if (passage.getStatus() != PassageStatus.SUSPENDED) {
            throw FileException.conflict("PASSAGE_NOT_SUSPENDED", "Passage is not suspended");
        }
        assertCanAct(passage, actor);

        Instant now = Instant.now();
        passage.setStatus(PassageStatus.IN_PROGRESS);
        passage.setResumedAt(now);
        if (passage.getDueAt() != null && passage.getSuspendedAt() != null) {
            long suspendedMillis = now.toEpochMilli() - passage.getSuspendedAt().toEpochMilli();
            passage.setDueAt(passage.getDueAt().plusMillis(suspendedMillis));
        }
        filePassageRepository.save(passage);

        file.setStatus(FileStatus.IN_PROGRESS);
        file.setExternalHoldReason(null);
        file.setExternalHoldSince(null);
        fileRepository.save(file);

        return PassageMapper.toCircuit(file, filePassageRepository.findByFileIdWithDetails(fileId), delaiService);
    }

    @Transactional
    public FilePassageCircuitResponse reassign(
            UUID fileId, UUID passageId, PassageReassignRequest request, SecurityUser actor) {
        FileEntity file = loadFile(fileId, actor);
        assertInProgress(file);
        FilePassage passage = loadPassage(fileId, passageId);
        assertActivePassage(passage);
        assertCanAct(passage, actor);

        User newResponsible = userRepository.findByIdWithOrganization(request.responsibleUserId())
                .orElseThrow(() -> FileException.badRequest("PASSAGE_USER_NOT_FOUND", "Responsible user not found"));
        if (!newResponsible.isActive()) {
            throw FileException.badRequest("PASSAGE_USER_INACTIVE", "Responsible user is inactive");
        }
        accessControlService.assertCanAccessOrganization(actor, newResponsible.getOrganization().getId());

        passage.setResponsibleUser(newResponsible);
        filePassageRepository.save(passage);
        return PassageMapper.toCircuit(file, filePassageRepository.findByFileIdWithDetails(fileId), delaiService);
    }

    @Transactional
    public FilePassageCircuitResponse updateInternalComment(
            UUID fileId, UUID passageId, PassageCommentRequest request, SecurityUser actor) {
        FileEntity file = loadFile(fileId, actor);
        FilePassage passage = loadPassage(fileId, passageId);
        assertCanAct(passage, actor);
        passage.setInternalComment(request.internalComment());
        filePassageRepository.save(passage);
        return PassageMapper.toCircuit(file, filePassageRepository.findByFileIdWithDetails(fileId), delaiService);
    }

    private void completePassage(FilePassage passage, String comment, Instant now) {
        passage.setStatus(PassageStatus.COMPLETED);
        passage.setTransmittedAt(now);
        if (StringUtils.hasText(comment)) {
            passage.setComment(comment.trim());
        }
        if (passage.getReceivedAt() != null) {
            passage.setConsumedHours(delaiService.calculateConsumedHours(
                    passage.getReceivedAt(),
                    now,
                    passage.getSuspendedAt(),
                    passage.getResumedAt()));
        }
        filePassageRepository.save(passage);
    }

    private void activateStage(
            FileEntity file,
            List<FilePassage> allPassages,
            int stageOrder,
            UUID nextResponsibleUserId,
            Instant now) {
        List<FilePassage> stagePassages = PassageStageHelper.passagesInStage(allPassages, stageOrder);
        boolean singleStepStage = stagePassages.size() == 1;
        for (FilePassage stagePassage : stagePassages) {
            UUID override = singleStepStage ? nextResponsibleUserId : null;
            activateSinglePassage(stagePassage, override, now);
        }
    }

    private void reactivateStage(List<FilePassage> allPassages, int stageOrder, Instant now) {
        for (FilePassage stagePassage : PassageStageHelper.passagesInStage(allPassages, stageOrder)) {
            stagePassage.setStatus(PassageStatus.IN_PROGRESS);
            stagePassage.setReceivedAt(now);
            stagePassage.setTransmittedAt(null);
            stagePassage.setReturnReason(null);
            stagePassage.setSuspendedAt(null);
            stagePassage.setResumedAt(null);
            stagePassage.setDueAt(delaiService.calculateDueDate(
                    now,
                    stagePassage.getChainStepTemplate().getDelayValue(),
                    stagePassage.getChainStepTemplate().getDelayUnit()));
            filePassageRepository.save(stagePassage);
        }
    }

    private void resetStageToPending(List<FilePassage> allPassages, int stageOrder) {
        for (FilePassage stagePassage : PassageStageHelper.passagesInStage(allPassages, stageOrder)) {
            if (stagePassage.getStatus() == PassageStatus.RETURNED) {
                continue;
            }
            stagePassage.setStatus(PassageStatus.PENDING);
            stagePassage.setReceivedAt(null);
            stagePassage.setTransmittedAt(null);
            stagePassage.setDueAt(null);
            stagePassage.setComment(null);
            stagePassage.setReturnReason(null);
            stagePassage.setSuspendedAt(null);
            stagePassage.setResumedAt(null);
            stagePassage.setConsumedHours(null);
            filePassageRepository.save(stagePassage);
        }
    }

    private void activateSinglePassage(FilePassage passage, UUID nextResponsibleUserId, Instant now) {
        ChainStepTemplate step = passage.getChainStepTemplate();
        User responsible;
        if (nextResponsibleUserId != null) {
            responsible = userRepository.findByIdWithOrganization(nextResponsibleUserId)
                    .orElseThrow(() -> FileException.badRequest("PASSAGE_USER_NOT_FOUND", "Responsible user not found"));
            if (!responsible.isActive()) {
                throw FileException.badRequest("PASSAGE_USER_INACTIVE", "Responsible user is inactive");
            }
        } else if (passage.getResponsibleUser() != null) {
            responsible = passage.getResponsibleUser();
        } else {
            throw FileException.badRequest(
                    "PASSAGE_USER_REQUIRED", "A responsible user must be chosen for the next step");
        }
        passage.setStatus(PassageStatus.IN_PROGRESS);
        passage.setReceivedAt(now);
        passage.setResponsibleUser(responsible);
        passage.setDueAt(delaiService.calculateDueDate(now, step.getDelayValue(), step.getDelayUnit()));
        filePassageRepository.save(passage);
    }

    private Map<UUID, UUID> toAssignmentMap(List<ChainStepAssignmentRequest> assignments) {
        Map<UUID, UUID> map = new HashMap<>();
        for (ChainStepAssignmentRequest assignment : assignments) {
            if (map.put(assignment.chainStepTemplateId(), assignment.responsibleUserId()) != null) {
                throw FileException.badRequest(
                        "PASSAGE_ASSIGNMENT_DUPLICATE", "Duplicate assignment for the same step");
            }
        }
        return map;
    }

    private Map<UUID, User> loadResponsibleUsers(Iterable<UUID> userIds) {
        Map<UUID, User> usersById = new HashMap<>();
        for (UUID userId : userIds) {
            if (usersById.containsKey(userId)) {
                continue;
            }
            User user = userRepository.findByIdWithOrganization(userId)
                    .orElseThrow(() -> FileException.badRequest("PASSAGE_USER_NOT_FOUND", "Responsible user not found"));
            if (!user.isActive()) {
                throw FileException.badRequest("PASSAGE_USER_INACTIVE", "Responsible user is inactive");
            }
            usersById.put(userId, user);
        }
        return usersById;
    }

    /** Remonte à la racine (ex. MINTP) puis inclut toute la descendance. */
    private List<UUID> collectMinistryOrganizationIds(Organization organization) {
        UUID rootId = organization.getId();
        UUID currentId = organization.getId();
        while (currentId != null) {
            Organization current = organizationRepository.findById(currentId).orElse(null);
            if (current == null) {
                break;
            }
            rootId = current.getId();
            if (current.getParent() == null) {
                break;
            }
            currentId = current.getParent().getId();
        }

        Set<UUID> ids = new LinkedHashSet<>();
        collectDescendants(rootId, ids);
        return new ArrayList<>(ids);
    }

    private void collectDescendants(UUID orgId, Set<UUID> ids) {
        ids.add(orgId);
        for (Organization child : organizationRepository.findByParentId(orgId)) {
            collectDescendants(child.getId(), ids);
        }
    }

    private FileEntity loadFile(UUID fileId, SecurityUser actor) {
        FileEntity file = fileRepository.findByIdWithDetails(fileId)
                .orElseThrow(() -> FileException.notFound("FILE_NOT_FOUND", "File not found"));
        accessControlService.assertCanAccessFile(actor, file);
        return file;
    }

    private FilePassage loadPassage(UUID fileId, UUID passageId) {
        return filePassageRepository.findByIdAndFileIdWithDetails(passageId, fileId)
                .orElseThrow(() -> FileException.notFound("PASSAGE_NOT_FOUND", "Passage not found"));
    }

    private FilePassage loadCurrentPassage(UUID fileId, SecurityUser actor) {
        loadFile(fileId, actor);
        List<FilePassage> active = filePassageRepository.findAllByFileIdAndStatusWithDetails(
                fileId, PassageStatus.IN_PROGRESS);
        if (active.isEmpty()) {
            active = filePassageRepository.findAllByFileIdAndStatusWithDetails(
                    fileId, PassageStatus.SUSPENDED);
        }
        if (active.isEmpty()) {
            throw FileException.notFound("PASSAGE_ACTIVE_NOT_FOUND", "No active passage for this file");
        }
        return active.get(0);
    }

    private void assertInProgress(FileEntity file) {
        if (file.getStatus() != FileStatus.IN_PROGRESS && file.getStatus() != FileStatus.ON_HOLD) {
            throw FileException.conflict("FILE_NOT_ACTIVE_WORKFLOW", "File is not in an active workflow state");
        }
    }

    private void assertActivePassage(FilePassage passage) {
        if (passage.getStatus() != PassageStatus.IN_PROGRESS) {
            throw FileException.conflict("PASSAGE_NOT_IN_PROGRESS", "Passage is not in progress");
        }
    }

    private void assertCanAct(FilePassage passage, SecurityUser actor) {
        if (!passageAuthorityService.canActOnPassage(actor, passage)) {
            throw FileException.forbidden("PASSAGE_ACCESS_DENIED", "You are not allowed to act on this passage");
        }
    }
}
