package com.nanotech.flux_pro_backend.repository;

import com.nanotech.flux_pro_backend.entity.OrganizationType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrganizationTypeRepository extends JpaRepository<OrganizationType, UUID> {

    Optional<OrganizationType> findByCode(String code);

    boolean existsByCode(String code);

    List<OrganizationType> findByActiveTrueOrderBySortOrderAsc();

    List<OrganizationType> findAllByOrderBySortOrderAsc();
}
