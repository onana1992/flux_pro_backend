package com.nanotech.flux_pro_backend.repository;

import com.nanotech.flux_pro_backend.entity.TenantSettings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TenantSettingsRepository extends JpaRepository<TenantSettings, UUID> {

    Optional<TenantSettings> findFirstByOrderByCreatedAtAsc();
}
