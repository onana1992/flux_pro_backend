package com.nanotech.flux_pro_backend.organisation;

import com.nanotech.flux_pro_backend.common.DtoMapper;
import com.nanotech.flux_pro_backend.organisation.dto.OrganisationRequest;
import com.nanotech.flux_pro_backend.organisation.dto.OrganisationTreeDto;
import com.nanotech.flux_pro_backend.security.OrganisationScopeService;
import com.nanotech.flux_pro_backend.security.SecurityUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrganisationService {

    private final OrganisationRepository organisationRepository;
    private final OrganisationScopeService organisationScopeService;

    @Transactional(readOnly = true)
    public List<OrganisationTreeDto> getTree(SecurityUser user) {
        List<Organisation> all = organisationRepository.findAllActive();
        if (user.getRole() == com.nanotech.flux_pro_backend.security.UserRole.SUPER_ADMIN
                || user.getRole() == com.nanotech.flux_pro_backend.security.UserRole.SG
                || user.getRole() == com.nanotech.flux_pro_backend.security.UserRole.CABINET) {
            return DtoMapper.buildTree(all);
        }
        return all.stream()
                .filter(o -> organisationScopeService.canAccess(user, o.getId()))
                .toList()
                .stream()
                .collect(java.util.stream.Collectors.collectingAndThen(
                        java.util.stream.Collectors.toList(),
                        DtoMapper::buildTree));
    }

    @Transactional(readOnly = true)
    public Organisation getById(UUID id, SecurityUser user) {
        Organisation org = organisationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Organisation introuvable"));
        if (!organisationScopeService.canAccess(user, id)) {
            throw new AccessDeniedException("Accès refusé à cette organisation");
        }
        return org;
    }

    @Transactional
    public Organisation create(OrganisationRequest request) {
        if (organisationRepository.existsByCode(request.code())) {
            throw new IllegalArgumentException("Code organisation déjà utilisé: " + request.code());
        }
        Organisation org = new Organisation();
        applyRequest(org, request);
        return organisationRepository.save(org);
    }

    @Transactional
    public Organisation update(UUID id, OrganisationRequest request) {
        Organisation org = organisationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Organisation introuvable"));
        if (!org.getCode().equals(request.code()) && organisationRepository.existsByCode(request.code())) {
            throw new IllegalArgumentException("Code organisation déjà utilisé: " + request.code());
        }
        applyRequest(org, request);
        return organisationRepository.save(org);
    }

    @Transactional
    public Organisation deactivate(UUID id) {
        Organisation org = organisationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Organisation introuvable"));
        org.setActif(false);
        return organisationRepository.save(org);
    }

    private void applyRequest(Organisation org, OrganisationRequest request) {
        org.setCode(request.code());
        org.setNom(request.nom());
        org.setType(request.type());
        org.setActif(request.actif());
        if (request.parentId() != null) {
            Organisation parent = organisationRepository.findById(request.parentId())
                    .orElseThrow(() -> new IllegalArgumentException("Organisation parente introuvable"));
            org.setParent(parent);
        } else {
            org.setParent(null);
        }
    }
}
