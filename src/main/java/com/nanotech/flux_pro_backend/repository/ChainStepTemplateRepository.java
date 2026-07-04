package com.nanotech.flux_pro_backend.repository;

import com.nanotech.flux_pro_backend.entity.ChainStepTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ChainStepTemplateRepository extends JpaRepository<ChainStepTemplate, UUID> {

    Optional<ChainStepTemplate> findByIdAndChainTemplateId(UUID id, UUID chainTemplateId);
}
