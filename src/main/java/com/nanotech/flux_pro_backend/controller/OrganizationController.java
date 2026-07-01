package com.nanotech.flux_pro_backend.controller;

import com.nanotech.flux_pro_backend.common.ImportResult;
import com.nanotech.flux_pro_backend.dto.request.OrganizationRequest;
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
import org.springframework.security.access.prepost.PreAuthorize;
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
    public OrganizationSummaryResponse getById(@PathVariable UUID id) {
        return DtoMapper.toSummary(organizationService.getById(id, securityUtils.currentUser()));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BUSINESS_ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public OrganizationSummaryResponse create(@Valid @RequestBody OrganizationRequest request) {
        return DtoMapper.toSummary(organizationService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BUSINESS_ADMIN')")
    public OrganizationSummaryResponse update(@PathVariable UUID id, @Valid @RequestBody OrganizationRequest request) {
        return DtoMapper.toSummary(organizationService.update(id, request));
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BUSINESS_ADMIN')")
    public OrganizationSummaryResponse deactivate(@PathVariable UUID id) {
        return DtoMapper.toSummary(organizationService.deactivate(id));
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ImportResult importCsv(@RequestParam("file") MultipartFile file) throws IOException {
        return organizationImportService.importCsv(file);
    }
}
