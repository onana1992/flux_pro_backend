package com.nanotech.flux_pro_backend.service;

import com.nanotech.flux_pro_backend.common.AppException;
import com.nanotech.flux_pro_backend.dto.request.FileTypeRequest;
import com.nanotech.flux_pro_backend.entity.FileType;
import com.nanotech.flux_pro_backend.repository.ChainTemplateRepository;
import com.nanotech.flux_pro_backend.repository.FileTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileTypeService {

    private final FileTypeRepository fileTypeRepository;
    private final ChainTemplateRepository chainTemplateRepository;

    @Transactional(readOnly = true)
    public List<FileType> listActive() {
        return fileTypeRepository.findByActiveTrueOrderBySortOrderAsc();
    }

    @Transactional(readOnly = true)
    public List<FileType> listAll() {
        return fileTypeRepository.findAllByOrderBySortOrderAsc();
    }

    @Transactional(readOnly = true)
    public FileType getById(UUID id) {
        return fileTypeRepository.findById(id)
                .orElseThrow(() -> AppException.notFound("FILE_TYPE_NOT_FOUND", "File type not found"));
    }

    @Transactional(readOnly = true)
    public FileType getByCode(String code) {
        return fileTypeRepository.findByCodeIgnoreCase(code)
                .orElseThrow(() -> AppException.notFound(
                        "FILE_TYPE_NOT_FOUND_BY_CODE", "File type not found: " + code, code));
    }

    @Transactional
    public FileType create(FileTypeRequest request) {
        if (fileTypeRepository.existsByCodeIgnoreCase(request.code())) {
            throw AppException.badRequest(
                    "FILE_TYPE_CODE_IN_USE", "File type code already in use: " + request.code(), request.code());
        }
        FileType type = new FileType();
        applyRequest(type, request, true);
        return fileTypeRepository.save(type);
    }

    @Transactional
    public FileType update(UUID id, FileTypeRequest request) {
        FileType type = getById(id);
        if (!type.getCode().equalsIgnoreCase(request.code())) {
            throw AppException.badRequest("FILE_TYPE_CODE_IMMUTABLE", "File type code cannot be changed");
        }
        applyRequest(type, request, false);
        return fileTypeRepository.save(type);
    }

    @Transactional
    public FileType deactivate(UUID id) {
        FileType type = getById(id);
        type.setActive(false);
        return fileTypeRepository.save(type);
    }

    @Transactional
    public void delete(UUID id) {
        FileType type = getById(id);
        if (chainTemplateRepository.existsByFileTypeCodeIgnoreCase(type.getCode())) {
            throw AppException.conflict(
                    "FILE_TYPE_LINKED_TO_CHAIN", "Cannot delete file type linked to a chain template");
        }
        fileTypeRepository.delete(type);
    }

    private void applyRequest(FileType type, FileTypeRequest request, boolean isCreate) {
        if (isCreate) {
            type.setCode(request.code().trim().toUpperCase());
        }
        type.setName(request.name().trim());
        type.setNameEn(request.nameEn() != null && !request.nameEn().isBlank() ? request.nameEn().trim() : null);
        type.setDescription(request.description());
        type.setDirectionCode(
                request.directionCode() != null && !request.directionCode().isBlank()
                        ? request.directionCode().trim().toUpperCase()
                        : null);
        type.setSortOrder(request.sortOrder() != null ? request.sortOrder() : 0);
        type.setActive(request.active());
    }
}
