package com.nanotech.flux_pro_backend.repository;

import com.nanotech.flux_pro_backend.entity.User;
import com.nanotech.flux_pro_backend.enumeration.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    @Query("""
            SELECT u FROM User u
            JOIN FETCH u.organization
            WHERE LOWER(u.email) = LOWER(:email)
            """)
    Optional<User> findByEmailWithOrganization(@Param("email") String email);

    @Query("""
            SELECT DISTINCT u FROM User u
            JOIN FETCH u.organization
            LEFT JOIN FETCH u.roles
            WHERE LOWER(u.email) = LOWER(:email)
            """)
    Optional<User> findByEmailWithRolesAndOrganization(@Param("email") String email);

    @Query("""
            SELECT DISTINCT u FROM User u
            JOIN FETCH u.organization
            LEFT JOIN FETCH u.roles
            WHERE u.id = :id
            """)
    Optional<User> findByIdWithRolesAndOrganization(@Param("id") UUID id);

    Optional<User> findByStaffNumber(String staffNumber);

    @Query("""
            SELECT u FROM User u
            JOIN FETCH u.organization
            WHERE u.id = :id
            """)
    Optional<User> findByIdWithOrganization(@Param("id") UUID id);

    @Query("""
            SELECT u FROM User u
            WHERE (:scopeAll = TRUE OR u.organization.id IN :organizationIds)
              AND (:organizationId IS NULL OR u.organization.id = :organizationId)
              AND (:role IS NULL OR u.role = :role)
              AND (:search IS NULL OR :search = '' OR
                   LOWER(u.lastName) LIKE LOWER(CONCAT('%', :search, '%')) OR
                   LOWER(u.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR
                   LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')) OR
                   LOWER(u.staffNumber) LIKE LOWER(CONCAT('%', :search, '%')))
            """)
    Page<User> search(
            @Param("scopeAll") boolean scopeAll,
            @Param("organizationIds") Collection<UUID> organizationIds,
            @Param("organizationId") UUID organizationId,
            @Param("role") UserRole role,
            @Param("search") String search,
            Pageable pageable);

    @Query("SELECT COUNT(u) > 0 FROM User u WHERE u.organization.id = :organizationId")
    boolean existsByOrganizationId(@Param("organizationId") UUID organizationId);

    @Query("""
            SELECT u FROM User u
            JOIN FETCH u.organization
            WHERE u.role = :role
              AND u.active = true
              AND u.organization.id IN :organizationIds
            ORDER BY u.lastName ASC, u.firstName ASC
            """)
    List<User> findActiveByRoleInOrganizations(
            @Param("role") UserRole role,
            @Param("organizationIds") Collection<UUID> organizationIds);

    @Query("""
            SELECT u FROM User u
            JOIN FETCH u.organization
            WHERE u.role = :role
              AND u.active = true
            ORDER BY u.lastName ASC, u.firstName ASC
            """)
    List<User> findActiveByRole(@Param("role") UserRole role);

    @Query("""
            SELECT u FROM User u
            JOIN FETCH u.organization
            WHERE u.organization.id = :organizationId
              AND u.active = true
            ORDER BY u.lastName ASC, u.firstName ASC
            """)
    List<User> findActiveByOrganizationId(@Param("organizationId") UUID organizationId);

    @Modifying
    @Query("""
            UPDATE User u
            SET u.organizationHead = false
            WHERE u.organization.id = :organizationId
              AND u.organizationHead = true
              AND (:excludeUserId IS NULL OR u.id <> :excludeUserId)
            """)
    void clearOrganizationHeadForOrganization(
            @Param("organizationId") UUID organizationId,
            @Param("excludeUserId") UUID excludeUserId);
}
