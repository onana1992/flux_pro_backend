package com.nanotech.flux_pro_backend.controller;

import com.nanotech.flux_pro_backend.dto.request.FileCancelRequest;
import com.nanotech.flux_pro_backend.dto.request.FileCloseRequest;
import com.nanotech.flux_pro_backend.dto.request.FileCreateRequest;
import com.nanotech.flux_pro_backend.dto.request.FileUpdateRequest;
import com.nanotech.flux_pro_backend.dto.response.FileAttachmentResponse;
import com.nanotech.flux_pro_backend.dto.response.FileDetailResponse;
import com.nanotech.flux_pro_backend.dto.response.FileSummaryResponse;
import com.nanotech.flux_pro_backend.enumeration.FilePriority;
import com.nanotech.flux_pro_backend.enumeration.FileStatus;
import com.nanotech.flux_pro_backend.entity.FileEntity;
import com.nanotech.flux_pro_backend.security.RbacPermissions;
import com.nanotech.flux_pro_backend.security.RequiresPermission;
import com.nanotech.flux_pro_backend.security.SecurityUtils;
import com.nanotech.flux_pro_backend.service.FileAttachmentService;
import com.nanotech.flux_pro_backend.service.FileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;
    private final FileAttachmentService fileAttachmentService;
    private final SecurityUtils securityUtils;

    @GetMapping
    @RequiresPermission(RbacPermissions.FILES_READ)
    public Page<FileSummaryResponse> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) UUID organizationId,
            @RequestParam(required = false) String fileTypeCode,
            @RequestParam(required = false) FileStatus status,
            @RequestParam(required = false) FilePriority priority,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate receivedFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate receivedTo,
            @PageableDefault(size = 20, sort = "receivedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return fileService.findAll(
                search, organizationId, fileTypeCode, status, priority,
                receivedFrom, receivedTo, pageable, securityUtils.currentUser());
    }

    @GetMapping("/{id}")
    @RequiresPermission(RbacPermissions.FILES_READ)
    public FileDetailResponse getById(@PathVariable UUID id) {
        return fileService.findById(id, securityUtils.currentUser());
    }

    @GetMapping("/by-reference/{ref}")
    @RequiresPermission(RbacPermissions.FILES_READ)
    public FileDetailResponse getByReference(@PathVariable String ref) {
        return fileService.findByReference(ref, securityUtils.currentUser());
    }

    @PostMapping
    @RequiresPermission(RbacPermissions.FILES_CREATE)
    @ResponseStatus(HttpStatus.CREATED)
    public FileDetailResponse create(@Valid @RequestBody FileCreateRequest request) {
        return fileService.create(request, securityUtils.currentUser());
    }

    @PutMapping("/{id}")
    @RequiresPermission(RbacPermissions.FILES_UPDATE)
    public FileDetailResponse update(@PathVariable UUID id, @Valid @RequestBody FileUpdateRequest request) {
        return fileService.update(id, request, securityUtils.currentUser());
    }

    @PostMapping("/{id}/submit")
    @RequiresPermission(RbacPermissions.FILES_UPDATE)
    public FileDetailResponse submit(@PathVariable UUID id) {
        return fileService.submit(id, securityUtils.currentUser());
    }

    @PatchMapping("/{id}/cancel")
    @RequiresPermission(RbacPermissions.FILES_UPDATE)
    public FileDetailResponse cancel(@PathVariable UUID id, @Valid @RequestBody FileCancelRequest request) {
        return fileService.cancel(id, request, securityUtils.currentUser());
    }

    @PatchMapping("/{id}/close")
    @RequiresPermission(RbacPermissions.FILES_CLOSE)
    public FileDetailResponse close(@PathVariable UUID id, @Valid @RequestBody FileCloseRequest request) {
        return fileService.close(id, request, securityUtils.currentUser());
    }

    @PatchMapping("/{id}/archive")
    @RequiresPermission(RbacPermissions.FILES_ARCHIVE)
    public FileDetailResponse archive(@PathVariable UUID id) {
        return fileService.archive(id, securityUtils.currentUser());
    }

    @DeleteMapping("/{id}")
    @RequiresPermission(RbacPermissions.FILES_DELETE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        fileService.deleteDraft(id, securityUtils.currentUser());
    }

    @PostMapping(value = "/{id}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @RequiresPermission(RbacPermissions.FILES_UPDATE)
    @ResponseStatus(HttpStatus.CREATED)
    public FileAttachmentResponse uploadAttachment(
            @PathVariable UUID id,
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "false") boolean responseDocument) {
        FileEntity fileEntity = fileService.loadForAttachment(id, securityUtils.currentUser());
        return fileAttachmentService.upload(fileEntity, file, responseDocument, securityUtils.currentUser());
    }

    @GetMapping("/{id}/attachments")
    @RequiresPermission(RbacPermissions.FILES_READ)
    public List<FileAttachmentResponse> listAttachments(@PathVariable UUID id) {
        FileEntity fileEntity = fileService.loadForAttachment(id, securityUtils.currentUser());
        return fileAttachmentService.listForFile(fileEntity);
    }

    @GetMapping("/{id}/attachments/{aid}/download")
    @RequiresPermission(RbacPermissions.FILES_READ)
    public ResponseEntity<Resource> downloadAttachment(@PathVariable UUID id, @PathVariable UUID aid) {
        FileEntity fileEntity = fileService.loadForAttachment(id, securityUtils.currentUser());
        Resource resource = fileAttachmentService.download(fileEntity, aid);
        String filename = fileAttachmentService.getOriginalFilename(fileEntity, aid);
        String contentType = fileAttachmentService.getContentType(fileEntity, aid);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(resource);
    }

    @DeleteMapping("/{id}/attachments/{aid}")
    @RequiresPermission(RbacPermissions.FILES_UPDATE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAttachment(@PathVariable UUID id, @PathVariable UUID aid) {
        FileEntity fileEntity = fileService.loadForAttachment(id, securityUtils.currentUser());
        fileAttachmentService.delete(fileEntity, aid);
    }
}
