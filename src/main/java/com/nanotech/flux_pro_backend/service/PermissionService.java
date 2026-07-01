package com.nanotech.flux_pro_backend.service;

import com.nanotech.flux_pro_backend.dto.request.CreatePermissionRequest;
import com.nanotech.flux_pro_backend.dto.request.UpdatePermissionRequest;
import com.nanotech.flux_pro_backend.entity.Permission;
import com.nanotech.flux_pro_backend.repository.PermissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PermissionService {

    private final PermissionRepository permissionRepository;

    @Transactional(readOnly = true)
    public Page<Permission> search(String resource, Pageable pageable) {
        return permissionRepository.search(resource, pageable);
    }

    @Transactional(readOnly = true)
    public Permission getById(UUID id) {
        return permissionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Permission not found"));
    }

    @Transactional
    public Permission create(CreatePermissionRequest request) {
        if (permissionRepository.existsByName(request.name())) {
            throw new IllegalArgumentException("Permission already exists");
        }
        Permission permission = new Permission();
        permission.setName(request.name());
        permission.setResource(request.resource());
        permission.setAction(request.action());
        permission.setDescription(request.description());
        return permissionRepository.save(permission);
    }

    @Transactional
    public Permission update(UUID id, UpdatePermissionRequest request) {
        Permission permission = getById(id);
        if (request.name() != null && !request.name().equals(permission.getName())) {
            if (permissionRepository.existsByName(request.name())) {
                throw new IllegalArgumentException("Permission name already in use");
            }
            permission.setName(request.name());
        }
        if (request.resource() != null) {
            permission.setResource(request.resource());
        }
        if (request.action() != null) {
            permission.setAction(request.action());
        }
        if (request.description() != null) {
            permission.setDescription(request.description());
        }
        return permissionRepository.save(permission);
    }

    @Transactional
    public void delete(UUID id) {
        Permission permission = getById(id);
        if (permissionRepository.countRoleLinks(id) > 0) {
            throw new IllegalArgumentException("Permission is assigned to roles");
        }
        permissionRepository.delete(permission);
    }
}
