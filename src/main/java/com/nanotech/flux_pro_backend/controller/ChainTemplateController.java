package com.nanotech.flux_pro_backend.controller;

import com.nanotech.flux_pro_backend.dto.request.ChainStepTemplateRequest;
import com.nanotech.flux_pro_backend.dto.request.ChainTemplateCreateRequest;
import com.nanotech.flux_pro_backend.dto.request.ChainTemplateUpdateRequest;
import com.nanotech.flux_pro_backend.dto.response.ChainTemplateDetailResponse;
import com.nanotech.flux_pro_backend.dto.response.ChainTemplateSummaryResponse;
import com.nanotech.flux_pro_backend.entity.ChainTemplate;
import com.nanotech.flux_pro_backend.mapper.ChainTemplateMapper;
import com.nanotech.flux_pro_backend.security.RbacPermissions;
import com.nanotech.flux_pro_backend.security.RequiresPermission;
import com.nanotech.flux_pro_backend.service.ChainTemplateService;
import com.nanotech.flux_pro_backend.service.ChainTemplateUsageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/chain-templates")
@RequiredArgsConstructor
public class ChainTemplateController {

    private final ChainTemplateService chainTemplateService;
    private final ChainTemplateUsageService chainTemplateUsageService;

    @GetMapping
    @RequiresPermission(RbacPermissions.CHAIN_TEMPLATES_READ)
    public Page<ChainTemplateSummaryResponse> list(
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) String fileTypeCode,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20) Pageable pageable) {
        return chainTemplateService.findAllSummaries(active, fileTypeCode, search, pageable);
    }

    @GetMapping("/{id}")
    @RequiresPermission(RbacPermissions.CHAIN_TEMPLATES_READ)
    public ChainTemplateDetailResponse getById(@PathVariable UUID id) {
        return toDetail(chainTemplateService.findById(id));
    }

    @GetMapping("/by-code/{code}")
    @RequiresPermission(RbacPermissions.CHAIN_TEMPLATES_READ)
    public ChainTemplateDetailResponse getByCode(@PathVariable String code) {
        return toDetail(chainTemplateService.findByCode(code));
    }

    @PostMapping
    @RequiresPermission(RbacPermissions.CHAIN_TEMPLATES_CREATE)
    @ResponseStatus(HttpStatus.CREATED)
    public ChainTemplateDetailResponse create(@Valid @RequestBody ChainTemplateCreateRequest request) {
        ChainTemplate created = chainTemplateService.create(request);
        return toDetail(chainTemplateService.findById(created.getId()));
    }

    @PutMapping("/{id}")
    @RequiresPermission(RbacPermissions.CHAIN_TEMPLATES_UPDATE)
    public ChainTemplateDetailResponse updateHeader(
            @PathVariable UUID id,
            @Valid @RequestBody ChainTemplateUpdateRequest request) {
        return toDetail(chainTemplateService.updateHeader(id, request));
    }

    @PutMapping("/{id}/steps")
    @RequiresPermission(RbacPermissions.CHAIN_TEMPLATES_UPDATE)
    public ChainTemplateDetailResponse replaceSteps(
            @PathVariable UUID id,
            @Valid @RequestBody List<ChainStepTemplateRequest> steps) {
        return toDetail(chainTemplateService.replaceSteps(id, steps));
    }

    @PatchMapping("/{id}/activate")
    @RequiresPermission(RbacPermissions.CHAIN_TEMPLATES_UPDATE)
    public ChainTemplateDetailResponse activate(@PathVariable UUID id) {
        return toDetail(chainTemplateService.activate(id));
    }

    @PatchMapping("/{id}/deactivate")
    @RequiresPermission(RbacPermissions.CHAIN_TEMPLATES_UPDATE)
    public ChainTemplateDetailResponse deactivate(@PathVariable UUID id) {
        return toDetail(chainTemplateService.deactivate(id));
    }

    @DeleteMapping("/{id}")
    @RequiresPermission(RbacPermissions.CHAIN_TEMPLATES_DELETE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        chainTemplateService.delete(id);
    }

    @PostMapping("/{id}/duplicate")
    @RequiresPermission(RbacPermissions.CHAIN_TEMPLATES_CREATE)
    @ResponseStatus(HttpStatus.CREATED)
    public ChainTemplateDetailResponse duplicate(@PathVariable UUID id) {
        ChainTemplate copy = chainTemplateService.duplicate(id);
        return toDetail(chainTemplateService.findById(copy.getId()));
    }

    private ChainTemplateDetailResponse toDetail(ChainTemplate template) {
        return ChainTemplateMapper.toDetail(
                template, chainTemplateUsageService.isAssociatedWithFiles(template.getId()));
    }
}
