package com.nanotech.flux_pro_backend.repository;

import com.nanotech.flux_pro_backend.entity.Permission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface PermissionRepository extends JpaRepository<Permission, UUID> {

    boolean existsByName(String name);

    Optional<Permission> findByName(String name);

    @Query("""
            SELECT p FROM Permission p
            WHERE (:resource IS NULL OR :resource = '' OR p.resource = :resource)
            ORDER BY p.resource, p.action
            """)
    Page<Permission> search(@Param("resource") String resource, Pageable pageable);

    @Query(value = "SELECT COUNT(*) FROM role_permissions WHERE permission_id = :permissionId", nativeQuery = true)
    long countRoleLinks(@Param("permissionId") UUID permissionId);
}
