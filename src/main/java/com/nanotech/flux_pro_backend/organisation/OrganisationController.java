package com.nanotech.flux_pro_backend.organisation;

import com.nanotech.flux_pro_backend.auth.dto.OrganisationSummaryDto;
import com.nanotech.flux_pro_backend.common.DtoMapper;
import com.nanotech.flux_pro_backend.common.ImportResult;
import com.nanotech.flux_pro_backend.organisation.dto.OrganisationRequest;
import com.nanotech.flux_pro_backend.organisation.dto.OrganisationTreeDto;
import com.nanotech.flux_pro_backend.security.SecurityUser;
import com.nanotech.flux_pro_backend.security.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
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

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/organisations")
@RequiredArgsConstructor
public class OrganisationController {

    private final OrganisationService organisationService;
    private final OrganisationImportService organisationImportService;
    private final SecurityUtils securityUtils;

    @GetMapping("/tree")
    public List<OrganisationTreeDto> getTree() {
        SecurityUser user = securityUtils.currentUser();
        return organisationService.getTree(user);
    }

    @GetMapping("/{id}")
    public OrganisationSummaryDto getById(@PathVariable UUID id) {
        SecurityUser user = securityUtils.currentUser();
        return DtoMapper.toSummary(organisationService.getById(id, user));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN_METIER')")
    @ResponseStatus(HttpStatus.CREATED)
    public OrganisationSummaryDto create(@Valid @RequestBody OrganisationRequest request) {
        return DtoMapper.toSummary(organisationService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN_METIER')")
    public OrganisationSummaryDto update(@PathVariable UUID id, @Valid @RequestBody OrganisationRequest request) {
        return DtoMapper.toSummary(organisationService.update(id, request));
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN_METIER')")
    public OrganisationSummaryDto deactivate(@PathVariable UUID id) {
        return DtoMapper.toSummary(organisationService.deactivate(id));
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ImportResult importCsv(@RequestParam("file") MultipartFile file) throws IOException {
        return organisationImportService.importCsv(file);
    }
}
