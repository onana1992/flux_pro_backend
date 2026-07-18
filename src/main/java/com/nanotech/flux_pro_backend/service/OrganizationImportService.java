package com.nanotech.flux_pro_backend.service;

import com.nanotech.flux_pro_backend.common.CsvUtils;
import com.nanotech.flux_pro_backend.common.ImportResult;
import com.nanotech.flux_pro_backend.common.MessageTranslator;
import com.nanotech.flux_pro_backend.entity.Organization;
import com.nanotech.flux_pro_backend.entity.OrganizationType;
import com.nanotech.flux_pro_backend.repository.OrganizationRepository;
import com.nanotech.flux_pro_backend.repository.OrganizationTypeRepository;
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
    private final OrganizationTypeRepository organizationTypeRepository;
    private final MessageTranslator messageTranslator;

    private String lineError(int line, String key, String fallback, Object... args) {
        String message = messageTranslator.translate(key, args, fallback);
        return messageTranslator.translate(
                "import.lineError", new Object[] {line, message}, "Line " + line + ": " + message);
    }

    @Transactional
    public ImportResult importCsv(MultipartFile file) throws IOException {
        List<Map<String, String>> rows = CsvUtils.parseSemicolonCsv(file.getInputStream());
        int created = 0;
        int updated = 0;
        List<String> errors = new ArrayList<>();

        Map<String, Organization> cache = new HashMap<>();
        organizationRepository.findAll().forEach(o -> cache.put(o.getCode(), o));

        Map<String, OrganizationType> typeCache = new HashMap<>();
        organizationTypeRepository.findAll().forEach(t -> typeCache.put(t.getCode(), t));

        for (int i = 0; i < rows.size(); i++) {
            Map<String, String> row = rows.get(i);
            int line = i + 2;
            try {
                String code = row.get("code");
                if (code == null || code.isBlank()) {
                    errors.add(lineError(line, "import.org.missingCode", "missing code"));
                    continue;
                }
                String typeCode = row.get("type");
                if (typeCode == null || typeCode.isBlank()) {
                    errors.add(lineError(line, "import.org.missingType", "missing type"));
                    continue;
                }
                OrganizationType orgType = typeCache.get(typeCode);
                if (orgType == null) {
                    orgType = organizationTypeRepository.findByCode(typeCode).orElse(null);
                }
                if (orgType == null) {
                    errors.add(lineError(line, "import.org.unknownType", "unknown type {0}", typeCode));
                    continue;
                }
                if (!orgType.isActive()) {
                    errors.add(lineError(line, "import.org.inactiveType", "inactive type {0}", typeCode));
                    continue;
                }
                typeCache.put(typeCode, orgType);

                Organization org = cache.get(code);
                boolean isNew = org == null;
                if (isNew) {
                    org = new Organization();
                    org.setCode(code);
                }
                org.setName(row.getOrDefault("nom", code));
                org.setOrganizationType(orgType);
                org.setActive(Boolean.parseBoolean(row.getOrDefault("actif", "true")));

                String parentCode = row.get("parent_code");
                if (parentCode != null && !parentCode.isBlank()) {
                    Organization parent = cache.get(parentCode);
                    if (parent == null) {
                        parent = organizationRepository.findByCode(parentCode).orElse(null);
                    }
                    if (parent == null) {
                        errors.add(lineError(line, "import.org.parentNotFound", "parent not found {0}", parentCode));
                        continue;
                    }
                    org.setParent(parent);
                } else {
                    if (!orgType.isAllowsRoot()) {
                        errors.add(lineError(
                                line, "import.org.parentRequired", "type {0} requires a parent", typeCode));
                        continue;
                    }
                    boolean otherRootExists = cache.values().stream()
                            .anyMatch(existing -> existing.getParent() == null && !code.equals(existing.getCode()));
                    if (otherRootExists) {
                        errors.add(lineError(
                                line, "import.org.rootExists", "a root organization already exists"));
                        continue;
                    }
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
                String message = messageTranslator.translate(e);
                errors.add(messageTranslator.translate(
                        "import.lineError", new Object[] {line, message}, "Line " + line + ": " + message));
            }
        }
        return new ImportResult(created, updated, errors);
    }
}
