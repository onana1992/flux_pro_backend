package com.nanotech.flux_pro_backend.repository;

import com.nanotech.flux_pro_backend.entity.SystemClockState;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SystemClockRepository extends JpaRepository<SystemClockState, UUID> {
}
