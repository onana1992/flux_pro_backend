package com.nanotech.flux_pro_backend.repository;

import com.nanotech.flux_pro_backend.entity.FileType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FileTypeRepository extends JpaRepository<FileType, UUID> {

    Optional<FileType> findByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCaseAndIdNot(String code, UUID id);

    List<FileType> findByActiveTrueOrderBySortOrderAsc();

    List<FileType> findAllByOrderBySortOrderAsc();
}
