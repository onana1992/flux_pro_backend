package com.nanotech.flux_pro_backend.controller;

import com.nanotech.flux_pro_backend.dto.request.PortalSubmissionCreateRequest;
import com.nanotech.flux_pro_backend.dto.response.FileAttachmentResponse;
import com.nanotech.flux_pro_backend.dto.response.PortalFormSchemaResponse;
import com.nanotech.flux_pro_backend.dto.response.PortalFormTypeResponse;
import com.nanotech.flux_pro_backend.dto.response.PortalSubmissionResponse;
import com.nanotech.flux_pro_backend.security.SecurityUtils;
import com.nanotech.flux_pro_backend.service.portal.PortalSubmissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/portal")
@RequiredArgsConstructor
public class PortalSubmissionController {

    private final PortalSubmissionService portalSubmissionService;
    private final SecurityUtils securityUtils;

    @GetMapping("/form-types")
    public List<PortalFormTypeResponse> listFormTypes() {
        return portalSubmissionService.listFormTypes(securityUtils.currentPortalUser());
    }

    @GetMapping("/form-types/{code}/schema")
    public PortalFormSchemaResponse getSchema(@PathVariable String code) {
        return portalSubmissionService.getFormSchema(code, securityUtils.currentPortalUser());
    }

    @PostMapping("/submissions")
    @ResponseStatus(HttpStatus.CREATED)
    public PortalSubmissionResponse create(@Valid @RequestBody PortalSubmissionCreateRequest request) {
        return portalSubmissionService.create(request, securityUtils.currentPortalUser());
    }

    @PostMapping("/submissions/{id}/submit")
    public PortalSubmissionResponse submit(@PathVariable UUID id) {
        return portalSubmissionService.submit(id, securityUtils.currentPortalUser());
    }

    @PostMapping(value = "/submissions/{id}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public FileAttachmentResponse uploadAttachment(
            @PathVariable UUID id,
            @RequestParam(required = false) String key,
            @RequestPart("file") MultipartFile file) {
        return portalSubmissionService.uploadAttachment(id, key, file, securityUtils.currentPortalUser());
    }

    @GetMapping("/submissions")
    public Page<PortalSubmissionResponse> listMine(Pageable pageable) {
        return portalSubmissionService.listMine(securityUtils.currentPortalUser(), pageable);
    }

    @GetMapping("/submissions/{ref}")
    public PortalSubmissionResponse getByRef(@PathVariable String ref) {
        // UUID → by id ; sinon référence métier
        try {
            UUID id = UUID.fromString(ref);
            return portalSubmissionService.getById(id, securityUtils.currentPortalUser());
        } catch (IllegalArgumentException ignored) {
            return portalSubmissionService.getByRef(ref, securityUtils.currentPortalUser());
        }
    }
}
