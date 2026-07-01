package com.nanotech.flux_pro_backend.repository;

import com.nanotech.flux_pro_backend.entity.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrganizationRepository extends JpaRepository<Organization, UUID> {

    Optional<Organization> findByCode(String code);

    boolean existsByCode(String code);

    boolean existsByOrganizationTypeId(UUID organizationTypeId);

    boolean existsByOrganizationTypeIdAndActiveTrue(UUID organizationTypeId);

    List<Organization> findByParentIsNull();

    List<Organization> findByParentId(UUID parentId);

    @Query("SELECT o FROM Organization o WHERE o.active = true")
    List<Organization> findAllActive();
}
