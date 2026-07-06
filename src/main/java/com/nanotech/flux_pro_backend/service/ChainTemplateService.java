package com.nanotech.flux_pro_backend.service;

import com.nanotech.flux_pro_backend.common.ChainTemplateException;
import com.nanotech.flux_pro_backend.dto.request.ChainStepTemplateRequest;
import com.nanotech.flux_pro_backend.dto.request.ChainTemplateCreateRequest;
import com.nanotech.flux_pro_backend.dto.request.ChainTemplateUpdateRequest;
import com.nanotech.flux_pro_backend.dto.response.ChainTemplateSummaryResponse;
import com.nanotech.flux_pro_backend.entity.ChainStepTemplate;
import com.nanotech.flux_pro_backend.entity.ChainTemplate;
import com.nanotech.flux_pro_backend.enumeration.DelayUnit;
import com.nanotech.flux_pro_backend.enumeration.UserRole;
import com.nanotech.flux_pro_backend.mapper.ChainTemplateMapper;
import com.nanotech.flux_pro_backend.repository.ChainTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChainTemplateService {

    static final int WORKING_HOURS_PER_DAY = 9;

    private final ChainTemplateRepository chainTemplateRepository;
    private final ChainTemplateUsageService chainTemplateUsageService;

    @Transactional(readOnly = true)
    public Page<ChainTemplateSummaryResponse> findAllSummaries(
            Boolean active, String fileTypeCode, String search, Pageable pageable) {
        return findAll(active, fileTypeCode, search, pageable).map(ChainTemplateMapper::toSummary);
    }

    @Transactional(readOnly = true)
    public Page<ChainTemplate> findAll(Boolean active, String fileTypeCode, String search, Pageable pageable) {
        return chainTemplateRepository.search(active, fileTypeCode, search, pageable);
    }

    @Transactional(readOnly = true)
    public ChainTemplate findById(UUID id) {
        return chainTemplateRepository.findByIdWithSteps(id)
                .orElseThrow(() -> ChainTemplateException.notFound(
                        "CHAIN_TEMPLATE_NOT_FOUND", "Chain template not found"));
    }

    @Transactional(readOnly = true)
    public ChainTemplate findByCode(String code) {
        return chainTemplateRepository.findByCodeWithSteps(code)
                .orElseThrow(() -> ChainTemplateException.notFound(
                        "CHAIN_TEMPLATE_NOT_FOUND_BY_CODE", "Chain template not found: " + code, code));
    }

    @Transactional
    public ChainTemplate create(ChainTemplateCreateRequest request) {
        if (chainTemplateRepository.existsByCodeIgnoreCase(request.code())) {
            throw ChainTemplateException.badRequest(
                    "CHAIN_TEMPLATE_CODE_EXISTS", "Chain template code already exists");
        }
        ChainTemplate template = new ChainTemplate();
        template.setCode(request.code().trim().toUpperCase());
        applyHeader(template, request.name(), request.description(), request.fileTypeCode(),
                request.totalDelayDays(), request.delayUnit());
        template.setSystemTemplate(false);
        template.setActive(true);
        validateSteps(template, request.steps());
        template.getSteps().addAll(toStepEntities(template, request.steps()));
        return chainTemplateRepository.save(template);
    }

    @Transactional
    public ChainTemplate updateHeader(UUID id, ChainTemplateUpdateRequest request) {
        ChainTemplate template = findById(id);
        applyHeader(template, request.name(), request.description(), request.fileTypeCode(),
                request.totalDelayDays(), request.delayUnit());
        validateSteps(template, toRequests(template.getSteps()));
        return chainTemplateRepository.save(template);
    }

    @Transactional
    public ChainTemplate replaceSteps(UUID id, List<ChainStepTemplateRequest> steps) {
        ChainTemplate template = findById(id);
        validateSteps(template, steps);
        template.getSteps().clear();
        template.getSteps().addAll(toStepEntities(template, steps));
        return chainTemplateRepository.save(template);
    }

    @Transactional
    public ChainTemplate activate(UUID id) {
        ChainTemplate template = findById(id);
        template.setActive(true);
        return chainTemplateRepository.save(template);
    }

    @Transactional
    public ChainTemplate deactivate(UUID id) {
        ChainTemplate template = findById(id);
        if (chainTemplateUsageService.hasInProgressFiles(id)) {
            throw ChainTemplateException.conflict(
                    "CHAIN_TEMPLATE_IN_USE", "Cannot deactivate chain template with in-progress files");
        }
        template.setActive(false);
        return chainTemplateRepository.save(template);
    }

    @Transactional
    public void delete(UUID id) {
        ChainTemplate template = findById(id);
        if (template.isSystemTemplate()) {
            throw ChainTemplateException.forbidden(
                    "CHAIN_SYSTEM_TEMPLATE_PROTECTED", "System chain templates cannot be deleted");
        }
        chainTemplateRepository.delete(template);
    }

    @Transactional
    public ChainTemplate duplicate(UUID id) {
        ChainTemplate source = findById(id);
        String baseCode = source.getCode() + "-COPY";
        String code = baseCode;
        int suffix = 1;
        while (chainTemplateRepository.existsByCodeIgnoreCase(code)) {
            code = baseCode + suffix++;
        }
        ChainTemplate copy = new ChainTemplate();
        copy.setCode(code);
        copy.setName(source.getName() + " (copie)");
        copy.setDescription(source.getDescription());
        copy.setFileTypeCode(source.getFileTypeCode());
        copy.setTotalDelayDays(source.getTotalDelayDays());
        copy.setDelayUnit(source.getDelayUnit());
        copy.setActive(true);
        copy.setSystemTemplate(false);
        List<ChainStepTemplateRequest> steps = source.getSteps().stream()
                .map(s -> new ChainStepTemplateRequest(
                        s.getStepOrder(),
                        s.getLabel(),
                        s.getResponsibleRole(),
                        s.getDelayValue(),
                        s.getDelayUnit(),
                        s.getExpectedAction(),
                        s.isOptional(),
                        s.isClosureStep()))
                .toList();
        copy.getSteps().addAll(toStepEntities(copy, steps));
        return chainTemplateRepository.save(copy);
    }

    void validateSteps(ChainTemplate template, List<ChainStepTemplateRequest> steps) {
        if (steps == null || steps.size() < 2) {
            throw ChainTemplateException.badRequest(
                    "CHAIN_STEP_MIN_COUNT", "At least two chain steps are required");
        }
        List<ChainStepTemplateRequest> ordered = steps.stream()
                .sorted(Comparator.comparingInt(ChainStepTemplateRequest::stepOrder))
                .toList();
        for (int i = 0; i < ordered.size(); i++) {
            if (ordered.get(i).stepOrder() != i + 1) {
                throw ChainTemplateException.badRequest(
                        "CHAIN_STEP_ORDER_GAP", "Chain step orders must be consecutive from 1 to N");
            }
        }
        long closureCount = ordered.stream().filter(ChainStepTemplateRequest::closureStep).count();
        if (closureCount != 1) {
            throw ChainTemplateException.badRequest(
                    "CHAIN_CLOSURE_STEP_INVALID", "Exactly one closure step is required");
        }
        for (ChainStepTemplateRequest step : ordered) {
            if (step.closureStep() && step.delayValue() != 0) {
                throw ChainTemplateException.badRequest(
                        "CHAIN_CLOSURE_DELAY_INVALID", "Closure step must have zero delay");
            }
            if (step.responsibleRole() == null || !isValidUserRole(step.responsibleRole())) {
                throw ChainTemplateException.badRequest(
                        "CHAIN_INVALID_ROLE", "Invalid responsible role: " + step.responsibleRole(),
                        step.responsibleRole());
            }
        }
        double sumDays = ordered.stream()
                .filter(s -> !s.closureStep())
                .mapToDouble(this::toWorkingDays)
                .sum();
        if (sumDays > template.getTotalDelayDays()) {
            throw ChainTemplateException.badRequest(
                    "CHAIN_DELAY_SUM_EXCEEDED",
                    "Sum of step delays exceeds total delay days (" + template.getTotalDelayDays() + ")",
                    template.getTotalDelayDays());
        }
    }

    private boolean isValidUserRole(UserRole role) {
        for (UserRole value : UserRole.values()) {
            if (value == role) {
                return true;
            }
        }
        return false;
    }

    double toWorkingDays(ChainStepTemplateRequest step) {
        if (step.delayUnit() == DelayUnit.WORKING_HOURS) {
            return (double) step.delayValue() / WORKING_HOURS_PER_DAY;
        }
        return step.delayValue();
    }

    private void applyHeader(
            ChainTemplate template,
            String name,
            String description,
            String fileTypeCode,
            int totalDelayDays,
            DelayUnit delayUnit) {
        template.setName(name.trim());
        template.setDescription(description);
        template.setFileTypeCode(fileTypeCode != null && !fileTypeCode.isBlank() ? fileTypeCode.trim() : null);
        template.setTotalDelayDays(totalDelayDays);
        template.setDelayUnit(delayUnit);
    }

    private List<ChainStepTemplate> toStepEntities(ChainTemplate template, List<ChainStepTemplateRequest> steps) {
        return steps.stream()
                .sorted(Comparator.comparingInt(ChainStepTemplateRequest::stepOrder))
                .map(req -> toStepEntity(template, req))
                .toList();
    }

    private ChainStepTemplate toStepEntity(ChainTemplate template, ChainStepTemplateRequest req) {
        ChainStepTemplate step = new ChainStepTemplate();
        step.setChainTemplate(template);
        step.setStepOrder(req.stepOrder());
        step.setLabel(req.label().trim());
        step.setResponsibleRole(req.responsibleRole());
        step.setDelayValue(req.delayValue());
        step.setDelayUnit(req.delayUnit());
        step.setExpectedAction(req.expectedAction());
        step.setOptional(req.optional());
        step.setClosureStep(req.closureStep());
        return step;
    }

    private List<ChainStepTemplateRequest> toRequests(List<ChainStepTemplate> steps) {
        return steps.stream()
                .map(s -> new ChainStepTemplateRequest(
                        s.getStepOrder(),
                        s.getLabel(),
                        s.getResponsibleRole(),
                        s.getDelayValue(),
                        s.getDelayUnit(),
                        s.getExpectedAction(),
                        s.isOptional(),
                        s.isClosureStep()))
                .toList();
    }
}
