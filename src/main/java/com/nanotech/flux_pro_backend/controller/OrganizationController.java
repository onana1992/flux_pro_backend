package com.nanotech.flux_pro_backend.controller;

import com.nanotech.flux_pro_backend.common.ImportResult;
import com.nanotech.flux_pro_backend.dto.request.OrganizationRequest;
import com.nanotech.flux_pro_backend.dto.response.OrganizationDetailResponse;
import com.nanotech.flux_pro_backend.dto.response.OrganizationSummaryResponse;
import com.nanotech.flux_pro_backend.dto.response.OrganizationTreeResponse;
import com.nanotech.flux_pro_backend.mapper.DtoMapper;
import com.nanotech.flux_pro_backend.security.SecurityUtils;
import com.nanotech.flux_pro_backend.service.OrganizationImportService;
import com.nanotech.flux_pro_backend.service.OrganizationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import com.nanotech.flux_pro_backend.security.RbacPermissions;
import com.nanotech.flux_pro_backend.security.RequiresPermission;
import org.springframework.security.access.prepost.PreAuthorize;
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
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/organizations")
@RequiredArgsConstructor
public class OrganizationController {

    private final OrganizationService organizationService;
    private final OrganizationImportService organizationImportService;
    private final SecurityUtils securityUtils;

    @GetMapping("/tree")
    public List<OrganizationTreeResponse> getTree() {
        return organizationService.getTree(securityUtils.currentUser());
    }

    @GetMapping("/{id}")
    public OrganizationDetailResponse getById(@PathVariable UUID id) {
        return organizationService.getDetailById(id, securityUtils.currentUser());
    }

    @PostMapping
    @RequiresPermission(RbacPermissions.ORGANIZATIONS_CREATE)
    @ResponseStatus(HttpStatus.CREATED)
    public OrganizationSummaryResponse create(@Valid @RequestBody OrganizationRequest request) {
        return DtoMapper.toSummary(organizationService.create(request));
    }

    @PutMapping("/{id}")
    @RequiresPermission(RbacPermissions.ORGANIZATIONS_UPDATE)
    public OrganizationSummaryResponse update(@PathVariable UUID id, @Valid @RequestBody OrganizationRequest request) {
        return DtoMapper.toSummary(organizationService.update(id, request));
    }

    @PatchMapping("/{id}/deactivate")
    @RequiresPermission(RbacPermissions.ORGANIZATIONS_UPDATE)
    public OrganizationSummaryResponse deactivate(@PathVariable UUID id) {
        return DtoMapper.toSummary(organizationService.deactivate(id));
    }

    @DeleteMapping("/{id}")
    @RequiresPermission(RbacPermissions.ORGANIZATIONS_DELETE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        organizationService.delete(id);
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @RequiresPermission(RbacPermissions.ORGANIZATIONS_IMPORT)
    public ImportResult importCsv(@RequestParam("file") MultipartFile file) throws IOException {
        return organizationImportService.importCsv(file);
    }
}
