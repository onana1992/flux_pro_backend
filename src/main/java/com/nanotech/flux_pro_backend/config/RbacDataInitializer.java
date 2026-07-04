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
                {RbacPermissions.CHAIN_TEMPLATES_READ, "CHAIN_TEMPLATES", "READ"},
                {RbacPermissions.CHAIN_TEMPLATES_CREATE, "CHAIN_TEMPLATES", "CREATE"},
                {RbacPermissions.CHAIN_TEMPLATES_UPDATE, "CHAIN_TEMPLATES", "UPDATE"},
                {RbacPermissions.CHAIN_TEMPLATES_DELETE, "CHAIN_TEMPLATES", "DELETE"},
                {RbacPermissions.FILE_TYPES_READ, "FILE_TYPES", "READ"},
                {RbacPermissions.FILE_TYPES_CREATE, "FILE_TYPES", "CREATE"},
                {RbacPermissions.FILE_TYPES_UPDATE, "FILE_TYPES", "UPDATE"},
                {RbacPermissions.FILE_TYPES_DELETE, "FILE_TYPES", "DELETE"},
                {RbacPermissions.FILES_READ, "FILES", "READ"},
                {RbacPermissions.FILES_CREATE, "FILES", "CREATE"},
                {RbacPermissions.FILES_UPDATE, "FILES", "UPDATE"},
                {RbacPermissions.FILES_CLOSE, "FILES", "CLOSE"},
                {RbacPermissions.FILES_ARCHIVE, "FILES", "ARCHIVE"},
                {RbacPermissions.FILES_DELETE, "FILES", "DELETE"},
                {RbacPermissions.FILES_TRANSMIT, "FILES", "TRANSMIT"},
                {RbacPermissions.ALERT_TYPES_READ, "ALERT_TYPES", "READ"},
                {RbacPermissions.ALERT_TYPES_CREATE, "ALERT_TYPES", "CREATE"},
                {RbacPermissions.ALERT_TYPES_UPDATE, "ALERT_TYPES", "UPDATE"},
                {RbacPermissions.ALERT_TYPES_DELETE, "ALERT_TYPES", "DELETE"},
                {RbacPermissions.ALERT_RULES_READ, "ALERT_RULES", "READ"},
                {RbacPermissions.ALERT_RULES_CREATE, "ALERT_RULES", "CREATE"},
                {RbacPermissions.ALERT_RULES_UPDATE, "ALERT_RULES", "UPDATE"},
                {RbacPermissions.ALERT_RULES_DELETE, "ALERT_RULES", "DELETE"},
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
        Set<String> chainRead = set(RbacPermissions.CHAIN_TEMPLATES_READ);
        Set<String> fileTypesRead = set(RbacPermissions.FILE_TYPES_READ);
        Set<String> alertRead = set(RbacPermissions.ALERT_TYPES_READ, RbacPermissions.ALERT_RULES_READ);
        Set<String> alertAdmin = set(
                RbacPermissions.ALERT_TYPES_READ,
                RbacPermissions.ALERT_TYPES_CREATE,
                RbacPermissions.ALERT_TYPES_UPDATE,
                RbacPermissions.ALERT_TYPES_DELETE,
                RbacPermissions.ALERT_RULES_READ,
                RbacPermissions.ALERT_RULES_CREATE,
                RbacPermissions.ALERT_RULES_UPDATE,
                RbacPermissions.ALERT_RULES_DELETE);
        Set<String> chainAdmin = set(
                RbacPermissions.CHAIN_TEMPLATES_READ,
                RbacPermissions.CHAIN_TEMPLATES_CREATE,
                RbacPermissions.CHAIN_TEMPLATES_UPDATE);
        Set<String> fileTypesAdmin = set(
                RbacPermissions.FILE_TYPES_READ,
                RbacPermissions.FILE_TYPES_CREATE,
                RbacPermissions.FILE_TYPES_UPDATE,
                RbacPermissions.FILE_TYPES_DELETE);
        Set<String> filesRead = set(RbacPermissions.FILES_READ);
        Set<String> filesAgent = set(
                RbacPermissions.FILES_READ,
                RbacPermissions.FILES_CREATE,
                RbacPermissions.FILES_UPDATE,
                RbacPermissions.FILES_TRANSMIT);
        Set<String> filesDirector = set(
                RbacPermissions.FILES_READ,
                RbacPermissions.FILES_CREATE,
                RbacPermissions.FILES_UPDATE,
                RbacPermissions.FILES_TRANSMIT,
                RbacPermissions.FILES_CLOSE,
                RbacPermissions.FILES_ARCHIVE);
        Set<String> filesRegionalDirector = set(
                RbacPermissions.FILES_READ,
                RbacPermissions.FILES_CREATE,
                RbacPermissions.FILES_UPDATE,
                RbacPermissions.FILES_TRANSMIT,
                RbacPermissions.FILES_CLOSE);
        Set<String> filesServiceHead = set(
                RbacPermissions.FILES_READ,
                RbacPermissions.FILES_CREATE,
                RbacPermissions.FILES_UPDATE,
                RbacPermissions.FILES_TRANSMIT);
        Map<String, Set<String>> matrix = new HashMap<>();
        matrix.put(UserRole.SUPER_ADMIN.name(), allPermissions());
        matrix.put(UserRole.BUSINESS_ADMIN.name(), merge(set(
                RbacPermissions.USERS_READ, RbacPermissions.USERS_CREATE, RbacPermissions.USERS_UPDATE,
                RbacPermissions.ORGANIZATIONS_READ, RbacPermissions.ORGANIZATIONS_CREATE,
                RbacPermissions.ORGANIZATIONS_UPDATE, RbacPermissions.ORGANIZATIONS_DELETE,
                RbacPermissions.ORGANIZATION_TYPES_READ, RbacPermissions.ORGANIZATION_TYPES_CREATE,
                RbacPermissions.ORGANIZATION_TYPES_UPDATE, RbacPermissions.ORGANIZATION_TYPES_DELETE,
                RbacPermissions.ROLES_READ, RbacPermissions.PERMISSIONS_READ),
                merge(chainAdmin, merge(fileTypesAdmin, merge(filesDirector, alertAdmin)))));
        matrix.put(UserRole.DIRECTOR.name(), merge(set(
                RbacPermissions.USERS_READ, RbacPermissions.ORGANIZATIONS_READ),
                merge(chainRead, merge(fileTypesRead, merge(filesDirector, alertRead)))));
        matrix.put(UserRole.SERVICE_HEAD.name(), merge(set(
                RbacPermissions.USERS_READ, RbacPermissions.ORGANIZATIONS_READ),
                merge(chainRead, merge(fileTypesRead, merge(filesServiceHead, alertRead)))));
        matrix.put(UserRole.REGIONAL_DIRECTOR.name(), merge(set(
                RbacPermissions.USERS_READ, RbacPermissions.ORGANIZATIONS_READ),
                merge(chainRead, merge(fileTypesRead, merge(filesRegionalDirector, alertRead)))));
        matrix.put(UserRole.SECRETARY_GENERAL.name(), merge(set(
                RbacPermissions.USERS_READ, RbacPermissions.ORGANIZATIONS_READ),
                merge(chainRead, merge(fileTypesRead, merge(filesRead, alertRead)))));
        matrix.put(UserRole.EXECUTIVE_OFFICE.name(), merge(set(
                RbacPermissions.USERS_READ, RbacPermissions.ORGANIZATIONS_READ),
                merge(chainRead, merge(fileTypesRead, merge(filesRead, alertRead)))));
        matrix.put(UserRole.AGENT.name(), merge(set(RbacPermissions.ORGANIZATIONS_READ),
                merge(chainRead, merge(fileTypesRead, filesAgent))));
        matrix.put(UserRole.SUPPORT.name(), merge(set(RbacPermissions.ORGANIZATIONS_READ),
                merge(chainRead, merge(fileTypesRead, filesAgent))));
        matrix.put(UserRole.READER.name(), merge(set(
                RbacPermissions.ORGANIZATIONS_READ, RbacPermissions.USERS_READ),
                merge(chainRead, merge(fileTypesRead, filesRead))));
        return matrix;
    }

    private Set<String> merge(Set<String> base, Set<String> extra) {
        Set<String> merged = new HashSet<>(base);
        merged.addAll(extra);
        return merged;
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
                RbacPermissions.LOGIN_AUDIT_READ,
                RbacPermissions.CHAIN_TEMPLATES_READ, RbacPermissions.CHAIN_TEMPLATES_CREATE,
                RbacPermissions.CHAIN_TEMPLATES_UPDATE, RbacPermissions.CHAIN_TEMPLATES_DELETE,
                RbacPermissions.FILE_TYPES_READ, RbacPermissions.FILE_TYPES_CREATE,
                RbacPermissions.FILE_TYPES_UPDATE, RbacPermissions.FILE_TYPES_DELETE,
                RbacPermissions.FILES_READ, RbacPermissions.FILES_CREATE,
                RbacPermissions.FILES_UPDATE, RbacPermissions.FILES_CLOSE,
                RbacPermissions.FILES_ARCHIVE, RbacPermissions.FILES_DELETE,
                RbacPermissions.FILES_TRANSMIT,
                RbacPermissions.ALERT_TYPES_READ, RbacPermissions.ALERT_TYPES_CREATE,
                RbacPermissions.ALERT_TYPES_UPDATE, RbacPermissions.ALERT_TYPES_DELETE,
                RbacPermissions.ALERT_RULES_READ, RbacPermissions.ALERT_RULES_CREATE,
                RbacPermissions.ALERT_RULES_UPDATE, RbacPermissions.ALERT_RULES_DELETE));
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
