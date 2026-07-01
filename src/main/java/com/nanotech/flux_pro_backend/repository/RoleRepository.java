package com.nanotech.flux_pro_backend.repository;

import com.nanotech.flux_pro_backend.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoleRepository extends JpaRepository<Role, UUID> {

    boolean existsByName(String name);

    Optional<Role> findByName(String name);

    @Query("""
            SELECT DISTINCT r FROM Role r
            LEFT JOIN FETCH r.permissions
            ORDER BY r.name
            """)
    List<Role> findAllWithPermissions();

    @Query("""
            SELECT r FROM Role r
            LEFT JOIN FETCH r.permissions
            WHERE r.id = :id
            """)
    Optional<Role> findByIdWithPermissions(@Param("id") UUID id);

    @Query("""
            SELECT DISTINCT r FROM Role r
            LEFT JOIN FETCH r.permissions
            WHERE r.id IN :ids
            """)
    List<Role> findAllByIdInWithPermissions(@Param("ids") Collection<UUID> ids);

    @Query("""
            SELECT r FROM Role r
            LEFT JOIN FETCH r.permissions
            WHERE r.name = :name
            """)
    Optional<Role> findByNameWithPermissions(@Param("name") String name);

    @Query(value = "SELECT COUNT(*) FROM user_roles WHERE role_id = :roleId", nativeQuery = true)
    long countUserLinks(@Param("roleId") UUID roleId);
}
