package com.nanotech.flux_pro_backend.controller;

import com.nanotech.flux_pro_backend.dto.request.OrganizationTypeRequest;
import com.nanotech.flux_pro_backend.dto.response.OrganizationTypeResponse;
import com.nanotech.flux_pro_backend.mapper.DtoMapper;
import com.nanotech.flux_pro_backend.service.OrganizationTypeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/organization-types")
@RequiredArgsConstructor
public class OrganizationTypeController {

    private final OrganizationTypeService organizationTypeService;

    @GetMapping
    public List<OrganizationTypeResponse> listActive() {
        return organizationTypeService.listActive().stream().map(DtoMapper::toTypeResponse).toList();
    }

    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BUSINESS_ADMIN')")
    public List<OrganizationTypeResponse> listAll() {
        return organizationTypeService.listAll().stream().map(DtoMapper::toTypeResponse).toList();
    }

    @GetMapping("/{id}")
    public OrganizationTypeResponse getById(@PathVariable UUID id) {
        return DtoMapper.toTypeResponse(organizationTypeService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BUSINESS_ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public OrganizationTypeResponse create(@Valid @RequestBody OrganizationTypeRequest request) {
        return DtoMapper.toTypeResponse(organizationTypeService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BUSINESS_ADMIN')")
    public OrganizationTypeResponse update(@PathVariable UUID id, @Valid @RequestBody OrganizationTypeRequest request) {
        return DtoMapper.toTypeResponse(organizationTypeService.update(id, request));
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BUSINESS_ADMIN')")
    public OrganizationTypeResponse deactivate(@PathVariable UUID id) {
        return DtoMapper.toTypeResponse(organizationTypeService.deactivate(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BUSINESS_ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        organizationTypeService.delete(id);
    }
}
