package com.nanotech.flux_pro_backend.service;

import com.nanotech.flux_pro_backend.common.CsvUtils;
import com.nanotech.flux_pro_backend.common.ImportResult;
import com.nanotech.flux_pro_backend.entity.Organization;
import com.nanotech.flux_pro_backend.enumeration.OrganizationType;
import com.nanotech.flux_pro_backend.repository.OrganizationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OrganizationImportService {

    private final OrganizationRepository organizationRepository;

    @Transactional
    public ImportResult importCsv(MultipartFile file) throws IOException {
        List<Map<String, String>> rows = CsvUtils.parseSemicolonCsv(file.getInputStream());
        int created = 0;
        int updated = 0;
        List<String> errors = new ArrayList<>();

        Map<String, Organization> cache = new HashMap<>();
        organizationRepository.findAll().forEach(o -> cache.put(o.getCode(), o));

        for (int i = 0; i < rows.size(); i++) {
            Map<String, String> row = rows.get(i);
            int line = i + 2;
            try {
                String code = row.get("code");
                if (code == null || code.isBlank()) {
                    errors.add("Line " + line + ": missing code");
                    continue;
                }
                Organization org = cache.get(code);
                boolean isNew = org == null;
                if (isNew) {
                    org = new Organization();
                    org.setCode(code);
                }
                org.setName(row.getOrDefault("nom", code));
                org.setType(OrganizationType.valueOf(row.get("type")));
                org.setActive(Boolean.parseBoolean(row.getOrDefault("actif", "true")));

                String parentCode = row.get("parent_code");
                if (parentCode != null && !parentCode.isBlank()) {
                    Organization parent = cache.get(parentCode);
                    if (parent == null) {
                        parent = organizationRepository.findByCode(parentCode).orElse(null);
                    }
                    if (parent == null) {
                        errors.add("Line " + line + ": parent not found " + parentCode);
                        continue;
                    }
                    org.setParent(parent);
                } else {
                    org.setParent(null);
                }

                org = organizationRepository.save(org);
                cache.put(code, org);
                if (isNew) {
                    created++;
                } else {
                    updated++;
                }
            } catch (Exception e) {
                errors.add("Line " + line + ": " + e.getMessage());
            }
        }
        return new ImportResult(created, updated, errors);
    }
}
