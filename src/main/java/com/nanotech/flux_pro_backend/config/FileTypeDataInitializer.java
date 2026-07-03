package com.nanotech.flux_pro_backend.config;

import com.nanotech.flux_pro_backend.entity.FileType;
import com.nanotech.flux_pro_backend.repository.FileTypeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Order(21)
@RequiredArgsConstructor
@Slf4j
public class FileTypeDataInitializer implements CommandLineRunner {

    private final FileTypeRepository fileTypeRepository;

    @Override
    @Transactional
    public void run(String... args) {
        try {
            seedIfAbsent("COUR-STD", () -> type(
                    "COUR-STD", "Courrier entrant standard",
                    "Courrier entrant — circuit standard DAG", "DAG", 10, true));
            seedIfAbsent("COUR-URG", () -> type(
                    "COUR-URG", "Courrier très urgent",
                    "Courrier entrant — circuit accéléré DAG", "DAG", 20, true));
            seedIfAbsent("MARCHE-SMP", () -> type(
                    "MARCHE-SMP", "Marché public simplifié",
                    "Marchés publics simplifiés DIER", "DIER", 30, true));
            seedIfAbsent("AUTH-TRAV", () -> type(
                    "AUTH-TRAV", "Autorisation travaux domaine public",
                    "Autorisations de travaux DRTP Centre", "DRTP-C", 40, true));
            seedIfAbsent("COOP-PART", () -> type(
                    "COOP-PART", "Coopération / partenariat",
                    "Coopération internationale — hors pilote actif", null, 50, false));
            log.info("File type reference data initialized");
        } catch (Exception e) {
            log.warn("File type initialization skipped — execute docs/sql/2026-07-02_file_types.sql: {}",
                    e.getMessage());
        }
    }

    private void seedIfAbsent(String code, java.util.function.Supplier<FileType> builder) {
        if (fileTypeRepository.findByCodeIgnoreCase(code).isEmpty()) {
            fileTypeRepository.save(builder.get());
        }
    }

    private FileType type(
            String code,
            String name,
            String description,
            String direction,
            int sortOrder,
            boolean active) {
        FileType ft = new FileType();
        ft.setCode(code);
        ft.setName(name);
        ft.setDescription(description);
        ft.setDirectionCode(direction);
        ft.setSortOrder(sortOrder);
        ft.setActive(active);
        return ft;
    }
}
