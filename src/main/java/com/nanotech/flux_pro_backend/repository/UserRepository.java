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
            LEFT JOIN FETCH u.substitute
            WHERE u.id = :id
            """)
    Optional<User> findByIdWithOrganization(@Param("id") UUID id);

    @Query("""
            SELECT DISTINCT u FROM User u
            JOIN FETCH u.organization
            WHERE u.id IN :ids
            """)
    List<User> findAllByIdWithOrganization(@Param("ids") Collection<UUID> ids);

    @Query("""
            SELECT u FROM User u
            LEFT JOIN FETCH u.substitute
            WHERE u.id = :id
            """)
    Optional<User> findByIdWithSubstitute(@Param("id") UUID id);

    @Query("""
            SELECT COUNT(u) > 0 FROM User u
            WHERE u.id = :titularId
              AND u.substitute.id = :substituteId
              AND u.substitute.active = true
            """)
    boolean isActiveSubstitute(
            @Param("titularId") UUID titularId,
            @Param("substituteId") UUID substituteId);

    @Query("""
            SELECT u.id FROM User u
            WHERE u.substitute.id = :substituteId
              AND u.active = true
            """)
    List<UUID> findActiveUserIdsBySubstituteId(@Param("substituteId") UUID substituteId);

    /**
     * Filtres texte via flags booléens + chaînes non-null — évite {@code LOWER(bytea)}
     * côté PostgreSQL quand Hibernate lie un {@code String} null en bytea.
     */
    @Query("""
            SELECT u FROM User u
            WHERE (:scopeAll = TRUE OR u.organization.id IN :organizationIds)
              AND (:hasOrganizationId = FALSE OR u.organization.id = :organizationId)
              AND (:restrictToOrgs = FALSE OR u.organization.id IN :restrictOrgIds)
              AND (:hasRole = FALSE OR u.role = :role)
              AND (
                   :searchEmpty = TRUE OR
                   LOWER(u.lastName) LIKE LOWER(CONCAT('%', :search, '%')) OR
                   LOWER(u.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR
                   LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')) OR
                   LOWER(u.staffNumber) LIKE LOWER(CONCAT('%', :search, '%')) OR
                   LOWER(CONCAT(COALESCE(u.firstName, ''), ' ', COALESCE(u.lastName, '')))
                        LIKE LOWER(CONCAT('%', :search, '%')) OR
                   LOWER(CONCAT(COALESCE(u.lastName, ''), ' ', COALESCE(u.firstName, '')))
                        LIKE LOWER(CONCAT('%', :search, '%')) OR
                   (:hasToken1 = TRUE AND (
                        LOWER(u.firstName) LIKE LOWER(CONCAT('%', :token1, '%')) OR
                        LOWER(u.lastName) LIKE LOWER(CONCAT('%', :token1, '%')) OR
                        LOWER(u.email) LIKE LOWER(CONCAT('%', :token1, '%')) OR
                        LOWER(u.staffNumber) LIKE LOWER(CONCAT('%', :token1, '%'))
                   ) AND (:hasToken2 = FALSE OR (
                        LOWER(u.firstName) LIKE LOWER(CONCAT('%', :token2, '%')) OR
                        LOWER(u.lastName) LIKE LOWER(CONCAT('%', :token2, '%')) OR
                        LOWER(u.email) LIKE LOWER(CONCAT('%', :token2, '%')) OR
                        LOWER(u.staffNumber) LIKE LOWER(CONCAT('%', :token2, '%'))
                   )))
              )
            """)
    Page<User> search(
            @Param("scopeAll") boolean scopeAll,
            @Param("organizationIds") Collection<UUID> organizationIds,
            @Param("hasOrganizationId") boolean hasOrganizationId,
            @Param("organizationId") UUID organizationId,
            @Param("restrictToOrgs") boolean restrictToOrgs,
            @Param("restrictOrgIds") Collection<UUID> restrictOrgIds,
            @Param("hasRole") boolean hasRole,
            @Param("role") UserRole role,
            @Param("searchEmpty") boolean searchEmpty,
            @Param("search") String search,
            @Param("hasToken1") boolean hasToken1,
            @Param("token1") String token1,
            @Param("hasToken2") boolean hasToken2,
            @Param("token2") String token2,
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
