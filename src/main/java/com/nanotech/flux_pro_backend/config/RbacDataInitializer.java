package com.nanotech.flux_pro_backend.config;

import com.nanotech.flux_pro_backend.entity.Permission;
import com.nanotech.flux_pro_backend.entity.Role;
import com.nanotech.flux_pro_backend.entity.User;
import com.nanotech.flux_pro_backend.enumeration.UserRole;
import com.nanotech.flux_pro_backend.repository.PermissionRepository;
import com.nanotech.flux_pro_backend.repository.RoleRepository;
import com.nanotech.flux_pro_backend.repository.UserRepository;
import com.nanotech.flux_pro_backend.security.RbacPermissions;
import com.nanotech.flux_pro_backend.service.RoleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class RbacDataInitializer implements CommandLineRunner {

    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final RoleService roleService;

    @Override
    @Transactional
    public void run(String... args) {
        try {
            seedPermissions();
            seedRoles();
            backfillUserRoles();
            log.info("RBAC reference data initialized");
        } catch (Exception e) {
            log.warn("RBAC initialization skipped — execute docs/sql/2026-07-02_rbac_roles_permissions.sql: {}", e.getMessage());
        }
    }

    private void seedPermissions() {
        String[][] definitions = {
                {RbacPermissions.USERS_READ, "USERS", "READ"},
                {RbacPermissions.USERS_CREATE, "USERS", "CREATE"},
                {RbacPermissions.USERS_UPDATE, "USERS", "UPDATE"},
                {RbacPermissions.USERS_DELETE, "USERS", "DELETE"},
                {RbacPermissions.USERS_IMPORT, "USERS", "IMPORT"},
                {RbacPermissions.USERS_RESET_PASSWORD, "USERS", "RESET_PASSWORD"},
                {RbacPermissions.USERS_UNLOCK, "USERS", "UNLOCK"},
                {RbacPermissions.ORGANIZATIONS_READ, "ORGANIZATIONS", "READ"},
                {RbacPermissions.ORGANIZATIONS_CREATE, "ORGANIZATIONS", "CREATE"},
                {RbacPermissions.ORGANIZATIONS_UPDATE, "ORGANIZATIONS", "UPDATE"},
                {RbacPermissions.ORGANIZATIONS_DELETE, "ORGANIZATIONS", "DELETE"},
                {RbacPermissions.ORGANIZATIONS_IMPORT, "ORGANIZATIONS", "IMPORT"},
                {RbacPermissions.ORGANIZATION_TYPES_READ, "ORGANIZATION_TYPES", "READ"},
                {RbacPermissions.ORGANIZATION_TYPES_CREATE, "ORGANIZATION_TYPES", "CREATE"},
                {RbacPermissions.ORGANIZATION_TYPES_UPDATE, "ORGANIZATION_TYPES", "UPDATE"},
                {RbacPermissions.ORGANIZATION_TYPES_DELETE, "ORGANIZATION_TYPES", "DELETE"},
                {RbacPermissions.ROLES_READ, "ROLES", "READ"},
                {RbacPermissions.ROLES_CREATE, "ROLES", "CREATE"},
                {RbacPermissions.ROLES_UPDATE, "ROLES", "UPDATE"},
                {RbacPermissions.ROLES_DELETE, "ROLES", "DELETE"},
                {RbacPermissions.PERMISSIONS_READ, "PERMISSIONS", "READ"},
                {RbacPermissions.PERMISSIONS_CREATE, "PERMISSIONS", "CREATE"},
                {RbacPermissions.PERMISSIONS_UPDATE, "PERMISSIONS", "UPDATE"},
                {RbacPermissions.PERMISSIONS_DELETE, "PERMISSIONS", "DELETE"},
                {RbacPermissions.LOGIN_AUDIT_READ, "LOGIN_AUDIT", "READ"},
        };
        for (String[] def : definitions) {
            if (!permissionRepository.existsByName(def[0])) {
                Permission permission = new Permission();
                permission.setName(def[0]);
                permission.setResource(def[1]);
                permission.setAction(def[2]);
                permission.setDescription(def[0]);
                permissionRepository.save(permission);
            }
        }
    }

    private void seedRoles() {
        Map<String, Set<String>> matrix = rolePermissionMatrix();
        for (UserRole userRole : UserRole.values()) {
            Role role = roleRepository.findByNameWithPermissions(userRole.name()).orElseGet(() -> {
                Role created = new Role();
                created.setName(userRole.name());
                created.setDescription("Rôle système " + userRole.name());
                created.setSystemRole(true);
                return roleRepository.save(created);
            });
            Set<String> expected = matrix.getOrDefault(userRole.name(), Set.of());
            List<Permission> permissions = permissionRepository.findAll().stream()
                    .filter(p -> expected.contains(p.getName()))
                    .toList();
            role.setPermissions(new ArrayList<>(permissions));
            role.setSystemRole(true);
            roleRepository.save(role);
        }
    }

    private Map<String, Set<String>> rolePermissionMatrix() {
        Map<String, Set<String>> matrix = new HashMap<>();
        matrix.put(UserRole.SUPER_ADMIN.name(), allPermissions());
        matrix.put(UserRole.BUSINESS_ADMIN.name(), set(
                RbacPermissions.USERS_READ, RbacPermissions.USERS_CREATE, RbacPermissions.USERS_UPDATE,
                RbacPermissions.ORGANIZATIONS_READ, RbacPermissions.ORGANIZATIONS_CREATE,
                RbacPermissions.ORGANIZATIONS_UPDATE, RbacPermissions.ORGANIZATIONS_DELETE,
                RbacPermissions.ORGANIZATION_TYPES_READ, RbacPermissions.ORGANIZATION_TYPES_CREATE,
                RbacPermissions.ORGANIZATION_TYPES_UPDATE, RbacPermissions.ORGANIZATION_TYPES_DELETE,
                RbacPermissions.ROLES_READ, RbacPermissions.PERMISSIONS_READ));
        matrix.put(UserRole.DIRECTOR.name(), set(
                RbacPermissions.USERS_READ, RbacPermissions.ORGANIZATIONS_READ));
        matrix.put(UserRole.SERVICE_HEAD.name(), set(
                RbacPermissions.USERS_READ, RbacPermissions.ORGANIZATIONS_READ));
        matrix.put(UserRole.REGIONAL_DIRECTOR.name(), set(
                RbacPermissions.USERS_READ, RbacPermissions.ORGANIZATIONS_READ));
        matrix.put(UserRole.SECRETARY_GENERAL.name(), set(
                RbacPermissions.USERS_READ, RbacPermissions.ORGANIZATIONS_READ));
        matrix.put(UserRole.EXECUTIVE_OFFICE.name(), set(
                RbacPermissions.USERS_READ, RbacPermissions.ORGANIZATIONS_READ));
        matrix.put(UserRole.AGENT.name(), set(RbacPermissions.ORGANIZATIONS_READ));
        matrix.put(UserRole.SUPPORT.name(), set(RbacPermissions.ORGANIZATIONS_READ));
        matrix.put(UserRole.READER.name(), set(
                RbacPermissions.ORGANIZATIONS_READ, RbacPermissions.USERS_READ));
        return matrix;
    }

    private Set<String> allPermissions() {
        return new HashSet<>(Arrays.asList(
                RbacPermissions.USERS_READ, RbacPermissions.USERS_CREATE, RbacPermissions.USERS_UPDATE,
                RbacPermissions.USERS_DELETE, RbacPermissions.USERS_IMPORT, RbacPermissions.USERS_RESET_PASSWORD,
                RbacPermissions.USERS_UNLOCK,
                RbacPermissions.ORGANIZATIONS_READ, RbacPermissions.ORGANIZATIONS_CREATE,
                RbacPermissions.ORGANIZATIONS_UPDATE, RbacPermissions.ORGANIZATIONS_DELETE,
                RbacPermissions.ORGANIZATIONS_IMPORT,
                RbacPermissions.ORGANIZATION_TYPES_READ, RbacPermissions.ORGANIZATION_TYPES_CREATE,
                RbacPermissions.ORGANIZATION_TYPES_UPDATE, RbacPermissions.ORGANIZATION_TYPES_DELETE,
                RbacPermissions.ROLES_READ, RbacPermissions.ROLES_CREATE, RbacPermissions.ROLES_UPDATE,
                RbacPermissions.ROLES_DELETE,
                RbacPermissions.PERMISSIONS_READ, RbacPermissions.PERMISSIONS_CREATE,
                RbacPermissions.PERMISSIONS_UPDATE, RbacPermissions.PERMISSIONS_DELETE,
                RbacPermissions.LOGIN_AUDIT_READ));
    }

    private Set<String> set(String... values) {
        return new HashSet<>(Arrays.asList(values));
    }

    private void backfillUserRoles() {
        for (User user : userRepository.findAll()) {
            roleService.syncPrimaryRole(user);
            userRepository.save(user);
        }
    }
}
