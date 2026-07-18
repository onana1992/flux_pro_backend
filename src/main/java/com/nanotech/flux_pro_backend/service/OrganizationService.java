package com.nanotech.flux_pro_backend.service;

import com.nanotech.flux_pro_backend.common.AppException;
import com.nanotech.flux_pro_backend.dto.request.OrganizationRequest;
import com.nanotech.flux_pro_backend.dto.response.OrganizationDetailResponse;
import com.nanotech.flux_pro_backend.dto.response.OrganizationTreeResponse;
import com.nanotech.flux_pro_backend.entity.Organization;
import com.nanotech.flux_pro_backend.entity.OrganizationType;
import com.nanotech.flux_pro_backend.enumeration.UserRole;
import com.nanotech.flux_pro_backend.mapper.DtoMapper;
import com.nanotech.flux_pro_backend.repository.FileNumberSequenceRepository;
import com.nanotech.flux_pro_backend.repository.FileRepository;
import com.nanotech.flux_pro_backend.repository.OrganizationRepository;
import com.nanotech.flux_pro_backend.repository.UserRepository;
import com.nanotech.flux_pro_backend.security.OrganizationScopeService;
import com.nanotech.flux_pro_backend.security.SecurityUser;
import com.nanotech.flux_pro_backend.security.TranslatableAccessDeniedException;
import lombok.RequiredArgsConstructor;
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
    private final UserRepository userRepository;
    private final FileRepository fileRepository;
    private final FileNumberSequenceRepository fileNumberSequenceRepository;

    @Transactional(readOnly = true)
    public List<OrganizationTreeResponse> getTree(SecurityUser user) {
        boolean fullTree = user.getRole() == UserRole.SUPER_ADMIN
                || user.getRole() == UserRole.BUSINESS_ADMIN
                || user.getRole() == UserRole.SECRETARY_GENERAL
                || user.getRole() == UserRole.EXECUTIVE_OFFICE;
        List<Organization> all = fullTree && (user.getRole() == UserRole.SUPER_ADMIN
                || user.getRole() == UserRole.BUSINESS_ADMIN)
                ? organizationRepository.findAll()
                : organizationRepository.findAllActive();
        if (user.getRole() == UserRole.SUPER_ADMIN
                || user.getRole() == UserRole.BUSINESS_ADMIN
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
        Organization org = organizationRepository.findByIdWithDetails(id)
                .orElseThrow(() -> AppException.notFound("ORGANIZATION_NOT_FOUND", "Organization not found"));
        if (!organizationScopeService.canAccess(user, id)) {
            throw new TranslatableAccessDeniedException(
                    "ACCESS_DENIED_ORGANIZATION", "Access denied to this organization");
        }
        return org;
    }

    @Transactional(readOnly = true)
    public OrganizationDetailResponse getDetailById(UUID id, SecurityUser user) {
        return DtoMapper.toDetail(getById(id, user));
    }

    @Transactional
    public Organization create(OrganizationRequest request) {
        if (organizationRepository.existsByCode(request.code())) {
            throw AppException.badRequest(
                    "ORGANIZATION_CODE_IN_USE", "Organization code already in use: " + request.code(),
                    request.code());
        }
        Organization org = new Organization();
        applyRequest(org, request);
        return organizationRepository.save(org);
    }

    @Transactional
    public Organization update(UUID id, OrganizationRequest request) {
        Organization org = organizationRepository.findById(id)
                .orElseThrow(() -> AppException.notFound("ORGANIZATION_NOT_FOUND", "Organization not found"));
        if (!org.getCode().equals(request.code()) && organizationRepository.existsByCode(request.code())) {
            throw AppException.badRequest(
                    "ORGANIZATION_CODE_IN_USE", "Organization code already in use: " + request.code(),
                    request.code());
        }
        applyRequest(org, request);
        return organizationRepository.save(org);
    }

    @Transactional
    public Organization deactivate(UUID id) {
        Organization org = organizationRepository.findById(id)
                .orElseThrow(() -> AppException.notFound("ORGANIZATION_NOT_FOUND", "Organization not found"));
        org.setActive(false);
        return organizationRepository.save(org);
    }

    @Transactional
    public void delete(UUID id) {
        Organization org = organizationRepository.findById(id)
                .orElseThrow(() -> AppException.notFound("ORGANIZATION_NOT_FOUND", "Organization not found"));
        if (org.getParent() == null) {
            throw AppException.conflict(
                    "ORGANIZATION_IS_ROOT", "Cannot delete the root organization");
        }
        if (organizationRepository.existsByParentId(id)) {
            throw AppException.conflict(
                    "ORGANIZATION_HAS_CHILDREN", "Cannot delete organization with child entities");
        }
        if (userRepository.existsByOrganizationId(id)) {
            throw AppException.conflict(
                    "ORGANIZATION_HAS_USERS", "Cannot delete organization with assigned users");
        }
        if (fileRepository.existsByOrganizationId(id)) {
            throw AppException.conflict(
                    "ORGANIZATION_HAS_FILES", "Cannot delete organization with associated files");
        }
        fileNumberSequenceRepository.deleteByOrganizationId(id);
        organizationRepository.delete(org);
    }

    private void applyRequest(Organization org, OrganizationRequest request) {
        OrganizationType type = organizationTypeService.getById(request.typeId());
        if (!type.isActive()) {
            throw AppException.badRequest(
                    "ORGANIZATION_TYPE_INACTIVE", "Organization type is inactive: " + type.getCode(),
                    type.getCode());
        }
        if (request.parentId() == null) {
            if (!type.isAllowsRoot()) {
                throw AppException.badRequest(
                        "ORGANIZATION_TYPE_CANNOT_BE_ROOT", "Organization type cannot be root: " + type.getCode(),
                        type.getCode());
            }
            if (organizationRepository.existsOtherRoot(org.getId())) {
                throw AppException.conflict(
                        "ORGANIZATION_ROOT_EXISTS", "A root organization already exists");
            }
        }
        org.setCode(request.code());
        org.setName(request.name());
        org.setOrganizationType(type);
        org.setActive(request.active());
        if (request.parentId() != null) {
            Organization parent = organizationRepository.findById(request.parentId())
                    .orElseThrow(() -> AppException.notFound(
                            "ORGANIZATION_PARENT_NOT_FOUND", "Parent organization not found"));
            org.setParent(parent);
        } else {
            org.setParent(null);
        }
    }
}
