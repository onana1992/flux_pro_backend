package com.nanotech.flux_pro_backend.mapper;

import com.nanotech.flux_pro_backend.dto.response.CurrentHolderResponse;
import com.nanotech.flux_pro_backend.dto.response.FilePassageCircuitResponse;
import com.nanotech.flux_pro_backend.dto.response.PassageCcUserResponse;
import com.nanotech.flux_pro_backend.dto.response.PassageResponse;
import com.nanotech.flux_pro_backend.entity.ChainStepTemplate;
import com.nanotech.flux_pro_backend.entity.FileEntity;
import com.nanotech.flux_pro_backend.entity.FilePassage;
import com.nanotech.flux_pro_backend.entity.User;
import com.nanotech.flux_pro_backend.enumeration.PassageStatus;
import com.nanotech.flux_pro_backend.service.DelaiService;
import com.nanotech.flux_pro_backend.service.PassageStageHelper;

import java.time.Instant;
import java.util.List;

public final class PassageMapper {

    private PassageMapper() {
    }

    public static FilePassageCircuitResponse toCircuit(
            FileEntity file, List<FilePassage> passages, DelaiService delaiService, Instant now) {
        List<FilePassage> active = PassageStageHelper.activePassages(passages);
        Integer currentStage = active.stream()
                .map(FilePassage::getStepOrder)
                .min(Integer::compareTo)
                .orElse(null);
        List<CurrentHolderResponse> currentHolders = active.stream()
                .map(p -> toCurrentHolder(p, delaiService, now))
                .toList();

        return new FilePassageCircuitResponse(
                file.getChainTemplate() != null ? file.getChainTemplate().getCode() : null,
                file.getChainTemplate() != null ? file.getChainTemplate().getName() : null,
                currentStage,
                currentHolders.isEmpty() ? null : currentHolders.get(0),
                currentHolders,
                passages.stream().map(p -> toPassage(p, delaiService, now)).toList());
    }

    public static PassageResponse toPassage(FilePassage passage, DelaiService delaiService, Instant now) {
        ChainStepTemplate step = passage.getChainStepTemplate();
        User responsible = passage.getResponsibleUser();
        String responsibleName = responsible != null
                ? responsible.getFirstName() + " " + responsible.getLastName()
                : null;
        String orgCode = responsible != null && responsible.getOrganization() != null
                ? responsible.getOrganization().getCode()
                : null;
        boolean overdue = passage.getDueAt() != null
                && (passage.getStatus() == PassageStatus.IN_PROGRESS
                        || passage.getStatus() == PassageStatus.SUSPENDED)
                && delaiService.isOverdue(passage.getDueAt(), now);
        Integer workingDaysHeld = null;
        if (passage.getReceivedAt() != null
                && (passage.getStatus() == PassageStatus.IN_PROGRESS
                        || passage.getStatus() == PassageStatus.SUSPENDED)) {
            workingDaysHeld = delaiService.countWorkingDays(passage.getReceivedAt(), now);
        }

        return new PassageResponse(
                passage.getId(),
                passage.getStepOrder(),
                step != null ? step.getLabel() : null,
                step != null ? step.getExpectedAction() : null,
                step != null && step.isOptional(),
                step != null && step.isClosureStep(),
                step != null ? step.getResponsibleRole() : null,
                step != null ? step.getDelayValue() : 0,
                step != null ? step.getDelayUnit() : null,
                passage.getStatus(),
                responsible != null ? responsible.getId() : null,
                responsibleName,
                responsible != null ? responsible.getEmail() : null,
                responsible != null ? responsible.getPhone() : null,
                responsible != null ? responsible.getJobTitle() : null,
                orgCode,
                responsible != null && responsible.getOrganization() != null
                        ? responsible.getOrganization().getName()
                        : null,
                passage.getReceivedAt(),
                passage.getTransmittedAt(),
                passage.getDueAt(),
                passage.getConsumedHours(),
                workingDaysHeld,
                overdue,
                passage.getComment(),
                passage.getInternalComment(),
                passage.getReturnReason(),
                passage.getSuspendedAt(),
                passage.getResumedAt(),
                toCcUsers(passage));
    }

    private static List<PassageCcUserResponse> toCcUsers(FilePassage passage) {
        if (passage.getCcUsers() == null || passage.getCcUsers().isEmpty()) {
            return List.of();
        }
        return passage.getCcUsers().stream()
                .map(u -> new PassageCcUserResponse(
                        u.getId(), u.getFirstName(), u.getLastName(), u.getEmail()))
                .toList();
    }

    public static CurrentHolderResponse toCurrentHolder(
            FilePassage passage, DelaiService delaiService, Instant now) {
        User responsible = passage.getResponsibleUser();
        ChainStepTemplate step = passage.getChainStepTemplate();
        Instant since = passage.getReceivedAt() != null ? passage.getReceivedAt() : now;
        boolean overdue = passage.getDueAt() != null && delaiService.isOverdue(passage.getDueAt(), now);
        return new CurrentHolderResponse(
                responsible != null ? responsible.getId() : null,
                responsible != null ? responsible.getFirstName() + " " + responsible.getLastName() : null,
                responsible != null && responsible.getOrganization() != null
                        ? responsible.getOrganization().getCode()
                        : null,
                step != null ? step.getLabel() : null,
                passage.getStepOrder(),
                since,
                delaiService.countWorkingDays(since, now),
                overdue,
                passage.getDueAt());
    }
}
