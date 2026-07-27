package com.nanotech.flux_pro_backend.repository;

import com.nanotech.flux_pro_backend.entity.PortalUser;
import com.nanotech.flux_pro_backend.enumeration.PortalUserType;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PortalUserRepository extends JpaRepository<PortalUser, UUID> {

    Optional<PortalUser> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    List<PortalUser> findByPortalUserTypeOrderByLastNameAscFirstNameAsc(PortalUserType portalUserType);

    @Query("""
            SELECT pu FROM PortalUser pu
            LEFT JOIN FETCH pu.organization
            WHERE pu.id = :id
            """)
    Optional<PortalUser> findByIdWithOrganization(@Param("id") UUID id);
}
