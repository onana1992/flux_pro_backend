package com.nanotech.flux_pro_backend.organisation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrganisationRepository extends JpaRepository<Organisation, UUID> {

    Optional<Organisation> findByCode(String code);

    boolean existsByCode(String code);

    List<Organisation> findByParentIsNull();

    List<Organisation> findByParentId(UUID parentId);

    @Query("SELECT o FROM Organisation o WHERE o.actif = true")
    List<Organisation> findAllActive();
}
