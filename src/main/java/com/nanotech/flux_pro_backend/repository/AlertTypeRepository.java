package com.nanotech.flux_pro_backend.repository;

import com.nanotech.flux_pro_backend.entity.AlertType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AlertTypeRepository extends JpaRepository<AlertType, UUID> {

    Optional<AlertType> findByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCaseAndIdNot(String code, UUID id);

    List<AlertType> findByActiveTrueOrderByLabelAsc();

    List<AlertType> findAllByOrderByLabelAsc();
}
