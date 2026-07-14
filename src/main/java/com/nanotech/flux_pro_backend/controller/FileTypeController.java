package com.nanotech.flux_pro_backend.controller;

import com.nanotech.flux_pro_backend.dto.request.FileTypeRequest;
import com.nanotech.flux_pro_backend.dto.response.FileTypeResponse;
import com.nanotech.flux_pro_backend.mapper.FileTypeMapper;
import com.nanotech.flux_pro_backend.security.RbacPermissions;
import com.nanotech.flux_pro_backend.security.RequiresPermission;
import com.nanotech.flux_pro_backend.service.FileTypeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class FileTypeController {

    private final FileTypeService fileTypeService;

    /** Liste active — consultation par tout agent authentifié (selects, création dossier). */
    @GetMapping("/api/file-types")
    public List<FileTypeResponse> listActive() {
        return fileTypeService.listActive().stream().map(FileTypeMapper::toResponse).toList();
    }

    @GetMapping("/api/admin/file-types")
    @RequiresPermission(RbacPermissions.FILE_TYPES_READ)
    public List<FileTypeResponse> listAll() {
        return fileTypeService.listAll().stream().map(FileTypeMapper::toResponse).toList();
    }

    @GetMapping("/api/admin/file-types/{id}")
    @RequiresPermission(RbacPermissions.FILE_TYPES_READ)
    public FileTypeResponse getById(@PathVariable UUID id) {
        return FileTypeMapper.toResponse(fileTypeService.getById(id));
    }

    @PostMapping("/api/admin/file-types")
    @RequiresPermission(RbacPermissions.FILE_TYPES_CREATE)
    @ResponseStatus(HttpStatus.CREATED)
    public FileTypeResponse create(@Valid @RequestBody FileTypeRequest request) {
        return FileTypeMapper.toResponse(fileTypeService.create(request));
    }

    @PutMapping("/api/admin/file-types/{id}")
    @RequiresPermission(RbacPermissions.FILE_TYPES_UPDATE)
    public FileTypeResponse update(@PathVariable UUID id, @Valid @RequestBody FileTypeRequest request) {
        return FileTypeMapper.toResponse(fileTypeService.update(id, request));
    }

    @PatchMapping("/api/admin/file-types/{id}/deactivate")
    @RequiresPermission(RbacPermissions.FILE_TYPES_UPDATE)
    public FileTypeResponse deactivate(@PathVariable UUID id) {
        return FileTypeMapper.toResponse(fileTypeService.deactivate(id));
    }

    @DeleteMapping("/api/admin/file-types/{id}")
    @RequiresPermission(RbacPermissions.FILE_TYPES_DELETE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        fileTypeService.delete(id);
    }
}
