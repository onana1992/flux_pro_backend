package com.nanotech.flux_pro_backend.security;

import com.nanotech.flux_pro_backend.entity.Role;
import com.nanotech.flux_pro_backend.entity.User;
import com.nanotech.flux_pro_backend.repository.RoleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class RbacAuthorityService {

    public record RbacAuthorities(List<String> roleNames, List<String> permissionNames) {
    }

    private final RoleRepository roleRepository;

    public RbacAuthorityService(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @Transactional(readOnly = true)
    public RbacAuthorities resolve(User user) {
        Set<String> roleNames = new LinkedHashSet<>();
        Set<String> permissionNames = new LinkedHashSet<>();

        if (user.getRoles() != null && !user.getRoles().isEmpty()) {
            List<UUID> roleIds = user.getRoles().stream().map(Role::getId).distinct().toList();
            for (Role role : user.getRoles()) {
                roleNames.add(role.getName());
            }
            roleRepository.findAllByIdInWithPermissions(roleIds).forEach(role ->
                    role.getPermissions().forEach(p -> permissionNames.add(p.getName())));
        } else {
            roleRepository.findByNameWithPermissions(user.getRole().name()).ifPresent(role -> {
                roleNames.add(role.getName());
                if (role.getPermissions() != null) {
                    role.getPermissions().forEach(p -> permissionNames.add(p.getName()));
                }
            });
            roleNames.add(user.getRole().name());
        }

        return new RbacAuthorities(new ArrayList<>(roleNames), new ArrayList<>(permissionNames));
    }
}
