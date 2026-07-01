package com.nanotech.flux_pro_backend.service;

import com.nanotech.flux_pro_backend.dto.request.AssignPermissionsRequest;
import com.nanotech.flux_pro_backend.dto.request.CreateRoleRequest;
import com.nanotech.flux_pro_backend.dto.request.UpdateRoleRequest;
import com.nanotech.flux_pro_backend.entity.Permission;
import com.nanotech.flux_pro_backend.entity.Role;
import com.nanotech.flux_pro_backend.entity.User;
import com.nanotech.flux_pro_backend.repository.PermissionRepository;
import com.nanotech.flux_pro_backend.repository.RoleRepository;
import com.nanotech.flux_pro_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<Role> listAll() {
        return roleRepository.findAllWithPermissions();
    }

    @Transactional(readOnly = true)
    public Role getById(UUID id) {
        return roleRepository.findByIdWithPermissions(id)
                .orElseThrow(() -> new IllegalArgumentException("Role not found"));
    }

    @Transactional
    public Role create(CreateRoleRequest request) {
        if (roleRepository.existsByName(request.name())) {
            throw new IllegalArgumentException("Role already exists");
        }
        Role role = new Role();
        role.setName(request.name());
        role.setDescription(request.description());
        role.setSystemRole(false);
        if (request.permissionIds() != null && !request.permissionIds().isEmpty()) {
            role.setPermissions(permissionRepository.findAllById(request.permissionIds()));
        }
        return roleRepository.save(role);
    }

    @Transactional
    public Role update(UUID id, UpdateRoleRequest request) {
        Role role = getById(id);
        if (role.isSystemRole() && request.name() != null && !request.name().equals(role.getName())) {
            throw new IllegalArgumentException("Cannot rename system role");
        }
        if (request.name() != null && !request.name().equals(role.getName())) {
            if (roleRepository.existsByName(request.name())) {
                throw new IllegalArgumentException("Role name already in use");
            }
            role.setName(request.name());
        }
        if (request.description() != null) {
            role.setDescription(request.description());
        }
        if (request.permissionIds() != null) {
            role.setPermissions(new ArrayList<>(permissionRepository.findAllById(request.permissionIds())));
        }
        return roleRepository.save(role);
    }

    @Transactional
    public void delete(UUID id) {
        Role role = getById(id);
        if (role.isSystemRole()) {
            throw new IllegalArgumentException("Cannot delete system role");
        }
        if (roleRepository.countUserLinks(id) > 0) {
            throw new IllegalArgumentException("Role is assigned to users");
        }
        roleRepository.delete(role);
    }

    @Transactional
    public void assignPermissions(UUID roleId, AssignPermissionsRequest request) {
        Role role = getById(roleId);
        Set<UUID> existing = new LinkedHashSet<>();
        role.getPermissions().forEach(p -> existing.add(p.getId()));
        for (Permission permission : permissionRepository.findAllById(request.permissionIds())) {
            if (!existing.contains(permission.getId())) {
                role.getPermissions().add(permission);
            }
        }
        roleRepository.save(role);
    }

    @Transactional
    public void revokePermission(UUID roleId, UUID permissionId) {
        Role role = getById(roleId);
        role.getPermissions().removeIf(p -> p.getId().equals(permissionId));
        roleRepository.save(role);
    }

    @Transactional
    public void assignRoleToUser(UUID userId, UUID roleId) {
        User user = userRepository.findByIdWithRolesAndOrganization(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        Role role = getById(roleId);
        boolean alreadyAssigned = user.getRoles().stream().anyMatch(r -> r.getId().equals(roleId));
        if (!alreadyAssigned) {
            user.getRoles().add(role);
            userRepository.save(user);
        }
    }

    @Transactional
    public void revokeRoleFromUser(UUID userId, UUID roleId) {
        User user = userRepository.findByIdWithRolesAndOrganization(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        if (user.getRoles().size() <= 1 && user.getRoles().stream().anyMatch(r -> r.getId().equals(roleId))) {
            throw new IllegalArgumentException("User must keep at least one role");
        }
        user.getRoles().removeIf(r -> r.getId().equals(roleId));
        userRepository.save(user);
    }

    @Transactional
    public void syncPrimaryRole(User user) {
        roleRepository.findByNameWithPermissions(user.getRole().name()).ifPresent(role -> {
            boolean hasRole = user.getRoles().stream().anyMatch(r -> r.getId().equals(role.getId()));
            if (!hasRole) {
                user.getRoles().add(role);
            }
        });
    }
}
