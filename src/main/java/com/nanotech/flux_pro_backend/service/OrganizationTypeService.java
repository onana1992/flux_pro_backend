package com.nanotech.flux_pro_backend.service;

import com.nanotech.flux_pro_backend.dto.request.OrganizationTypeRequest;
import com.nanotech.flux_pro_backend.entity.OrganizationType;
import com.nanotech.flux_pro_backend.mapper.DtoMapper;
import com.nanotech.flux_pro_backend.repository.OrganizationRepository;
import com.nanotech.flux_pro_backend.repository.OrganizationTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrganizationTypeService {

    private final OrganizationTypeRepository organizationTypeRepository;
    private final OrganizationRepository organizationRepository;

    @Transactional(readOnly = true)
    public List<OrganizationType> listActive() {
        return dedupeByCode(organizationTypeRepository.findByActiveTrueOrderBySortOrderAsc());
    }

    @Transactional(readOnly = true)
    public List<OrganizationType> listAll() {
        return organizationTypeRepository.findAllByOrderBySortOrderAsc();
    }

    private static List<OrganizationType> dedupeByCode(List<OrganizationType> types) {
        return types.stream()
                .collect(java.util.stream.Collectors.toMap(
                        OrganizationType::getCode,
                        t -> t,
                        (existing, duplicate) -> existing,
                        java.util.LinkedHashMap::new))
                .values()
                .stream()
                .toList();
    }

    @Transactional(readOnly = true)
    public OrganizationType getById(UUID id) {
        return organizationTypeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Organization type not found"));
    }

    @Transactional(readOnly = true)
    public OrganizationType getByCode(String code) {
        return organizationTypeRepository.findByCode(code)
                .orElseThrow(() -> new IllegalArgumentException("Organization type not found: " + code));
    }

    @Transactional
    public OrganizationType create(OrganizationTypeRequest request) {
        if (organizationTypeRepository.existsByCode(request.code())) {
            throw new IllegalArgumentException("Organization type code already in use: " + request.code());
        }
        OrganizationType type = new OrganizationType();
        applyRequest(type, request, true);
        return organizationTypeRepository.save(type);
    }

    @Transactional
    public OrganizationType update(UUID id, OrganizationTypeRequest request) {
        OrganizationType type = getById(id);
        if (!type.getCode().equals(request.code())) {
            throw new IllegalArgumentException("Organization type code cannot be changed");
        }
        applyRequest(type, request, false);
        return organizationTypeRepository.save(type);
    }

    @Transactional
    public OrganizationType deactivate(UUID id) {
        OrganizationType type = getById(id);
        if (organizationRepository.existsByOrganizationTypeIdAndActiveTrue(id)) {
            throw new IllegalStateException("Cannot deactivate type used by active organizations");
        }
        type.setActive(false);
        return organizationTypeRepository.save(type);
    }

    @Transactional
    public void delete(UUID id) {
        OrganizationType type = getById(id);
        if (organizationRepository.existsByOrganizationTypeId(id)) {
            throw new IllegalArgumentException("Cannot delete type used by organizations");
        }
        organizationTypeRepository.delete(type);
    }

    private void applyRequest(OrganizationType type, OrganizationTypeRequest request, boolean isCreate) {
        if (isCreate) {
            type.setCode(request.code());
        }
        type.setName(request.name());
        type.setNameEn(request.nameEn());
        type.setDescription(request.description());
        type.setColor(request.color());
        type.setSortOrder(request.sortOrder() != null ? request.sortOrder() : 0);
        type.setAllowsRoot(request.allowsRoot());
        type.setRegionalScope(request.isRegionalScope());
        type.setActive(request.active());
    }
}
