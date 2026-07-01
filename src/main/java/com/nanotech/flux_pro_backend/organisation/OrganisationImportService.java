package com.nanotech.flux_pro_backend.organisation;

import com.nanotech.flux_pro_backend.common.CsvUtils;
import com.nanotech.flux_pro_backend.common.ImportResult;
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
public class OrganisationImportService {

    private final OrganisationRepository organisationRepository;

    @Transactional
    public ImportResult importCsv(MultipartFile file) throws IOException {
        List<Map<String, String>> rows = CsvUtils.parseSemicolonCsv(file.getInputStream());
        int created = 0;
        int updated = 0;
        List<String> errors = new ArrayList<>();

        Map<String, Organisation> cache = new HashMap<>();
        organisationRepository.findAll().forEach(o -> cache.put(o.getCode(), o));

        for (int i = 0; i < rows.size(); i++) {
            Map<String, String> row = rows.get(i);
            int line = i + 2;
            try {
                String code = row.get("code");
                if (code == null || code.isBlank()) {
                    errors.add("Ligne " + line + ": code manquant");
                    continue;
                }
                Organisation org = cache.get(code);
                boolean isNew = org == null;
                if (isNew) {
                    org = new Organisation();
                    org.setCode(code);
                }
                org.setNom(row.getOrDefault("nom", code));
                org.setType(OrganisationType.valueOf(row.get("type")));
                org.setActif(Boolean.parseBoolean(row.getOrDefault("actif", "true")));

                String parentCode = row.get("parent_code");
                if (parentCode != null && !parentCode.isBlank()) {
                    Organisation parent = cache.get(parentCode);
                    if (parent == null) {
                        parent = organisationRepository.findByCode(parentCode).orElse(null);
                    }
                    if (parent == null) {
                        errors.add("Ligne " + line + ": parent introuvable " + parentCode);
                        continue;
                    }
                    org.setParent(parent);
                } else {
                    org.setParent(null);
                }

                org = organisationRepository.save(org);
                cache.put(code, org);
                if (isNew) {
                    created++;
                } else {
                    updated++;
                }
            } catch (Exception e) {
                errors.add("Ligne " + line + ": " + e.getMessage());
            }
        }
        return new ImportResult(created, updated, errors);
    }
}
