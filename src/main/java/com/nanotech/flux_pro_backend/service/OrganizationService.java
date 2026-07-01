package com.nanotech.flux_pro_backend.service;

import com.nanotech.flux_pro_backend.dto.request.OrganizationRequest;
import com.nanotech.flux_pro_backend.dto.response.OrganizationTreeResponse;
import com.nanotech.flux_pro_backend.entity.Organization;
import com.nanotech.flux_pro_backend.entity.OrganizationType;
import com.nanotech.flux_pro_backend.enumeration.UserRole;
import com.nanotech.flux_pro_backend.mapper.DtoMapper;
import com.nanotech.flux_pro_backend.repository.OrganizationRepository;
import com.nanotech.flux_pro_backend.security.OrganizationScopeService;
import com.nanotech.flux_pro_backend.security.SecurityUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final OrganizationTypeService organizationTypeService;
    private final OrganizationScopeService organizationScopeService;

    @Transactional(readOnly = true)
    public List<OrganizationTreeResponse> getTree(SecurityUser user) {
        List<Organization> all = organizationRepository.findAllActive();
        if (user.getRole() == UserRole.SUPER_ADMIN
                || user.getRole() == UserRole.SECRETARY_GENERAL
                || user.getRole() == UserRole.EXECUTIVE_OFFICE) {
            return DtoMapper.buildTree(all);
        }
        return all.stream()
                .filter(o -> organizationScopeService.canAccess(user, o.getId()))
                .toList()
                .stream()
                .collect(java.util.stream.Collectors.collectingAndThen(
                        java.util.stream.Collectors.toList(),
                        DtoMapper::buildTree));
    }

    @Transactional(readOnly = true)
    public Organization getById(UUID id, SecurityUser user) {
        Organization org = organizationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Organization not found"));
        if (!organizationScopeService.canAccess(user, id)) {
            throw new AccessDeniedException("Access denied to this organization");
        }
        return org;
    }

    @Transactional
    public Organization create(OrganizationRequest request) {
        if (organizationRepository.existsByCode(request.code())) {
            throw new IllegalArgumentException("Organization code already in use: " + request.code());
        }
        Organization org = new Organization();
        applyRequest(org, request);
        return organizationRepository.save(org);
    }

    @Transactional
    public Organization update(UUID id, OrganizationRequest request) {
        Organization org = organizationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Organization not found"));
        if (!org.getCode().equals(request.code()) && organizationRepository.existsByCode(request.code())) {
            throw new IllegalArgumentException("Organization code already in use: " + request.code());
        }
        applyRequest(org, request);
        return organizationRepository.save(org);
    }

    @Transactional
    public Organization deactivate(UUID id) {
        Organization org = organizationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Organization not found"));
        org.setActive(false);
        return organizationRepository.save(org);
    }

    private void applyRequest(Organization org, OrganizationRequest request) {
        OrganizationType type = organizationTypeService.getById(request.typeId());
        if (!type.isActive()) {
            throw new IllegalArgumentException("Organization type is inactive: " + type.getCode());
        }
        if (request.parentId() == null && !type.isAllowsRoot()) {
            throw new IllegalArgumentException("Organization type cannot be root: " + type.getCode());
        }
        org.setCode(request.code());
        org.setName(request.name());
        org.setOrganizationType(type);
        org.setActive(request.active());
        if (request.parentId() != null) {
            Organization parent = organizationRepository.findById(request.parentId())
                    .orElseThrow(() -> new IllegalArgumentException("Parent organization not found"));
            org.setParent(parent);
        } else {
            org.setParent(null);
        }
    }
}
