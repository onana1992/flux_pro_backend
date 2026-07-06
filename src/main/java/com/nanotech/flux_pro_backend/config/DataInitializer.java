package com.nanotech.flux_pro_backend.config;

import com.nanotech.flux_pro_backend.entity.Organization;
import com.nanotech.flux_pro_backend.entity.OrganizationType;
import com.nanotech.flux_pro_backend.entity.User;
import com.nanotech.flux_pro_backend.enumeration.UserRole;
import com.nanotech.flux_pro_backend.repository.OrganizationRepository;
import com.nanotech.flux_pro_backend.repository.OrganizationTypeRepository;
import com.nanotech.flux_pro_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final OrganizationTypeRepository organizationTypeRepository;
    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /** Aligné sur docs/sql/2026-07-01_organization_types_table.sql */
    private static final String[][] TYPE_SEED = {
            {"00000000-0000-4000-8000-000000000001", "MINISTRY", "Ministère", "Ministry", "purple", "1", "true", "false"},
            {"00000000-0000-4000-8000-000000000002", "DIRECTORATE", "Direction", "Directorate", "blue", "2", "false", "false"},
            {"00000000-0000-4000-8000-000000000003", "DIVISION", "Division", "Division", "gray", "3", "false", "false"},
            {"00000000-0000-4000-8000-000000000004", "SERVICE", "Service", "Service", "green", "4", "false", "false"},
            {"00000000-0000-4000-8000-000000000005", "REGIONAL_DIRECTORATE", "DRTP", "Regional directorate", "orange", "5", "false", "true"},
    };

    private static final String[][] DRTP_SEED = {
            {"DRTP-ADAMAOUA", "DRTP Adamaoua (Ngaoundéré)"},
            {"DRTP-C", "DRTP du Centre (Yaoundé)"},
            {"DRTP-EST", "DRTP Est (Bertoua)"},
            {"DRTP-EXTN", "DRTP Extrême-Nord (Maroua)"},
            {"DRTP-LITTORAL", "DRTP Littoral (Douala)"},
            {"DRTP-NORD", "DRTP Nord (Garoua)"},
            {"DRTP-NO", "DRTP Nord-Ouest (Bamenda)"},
            {"DRTP-OUEST", "DRTP Ouest (Bafoussam)"},
            {"DRTP-SUD", "DRTP Sud (Ebolowa)"},
            {"DRTP-SO", "DRTP Sud-Ouest (Buea)"}
    };

    @Override
    @Transactional
    public void run(String... args) {
        seedOrganizationTypes();

        OrganizationType ministryType = organizationTypeRepository.findByCode("MINISTRY").orElseThrow();
        OrganizationType directorateType = organizationTypeRepository.findByCode("DIRECTORATE").orElseThrow();
        OrganizationType regionalType = organizationTypeRepository.findByCode("REGIONAL_DIRECTORATE").orElseThrow();

        Organization mintp = organizationRepository.findByCode("MINTP").orElseGet(() -> {
            Organization root = new Organization();
            root.setCode("MINTP");
            root.setName("Ministère des Travaux Publics");
            root.setOrganizationType(ministryType);
            root.setActive(true);
            log.info("Seed: created MINTP organization");
            return organizationRepository.save(root);
        });

        for (String[] drtp : DRTP_SEED) {
            if (organizationRepository.findByCode(drtp[0]).isEmpty()) {
                Organization org = new Organization();
                org.setCode(drtp[0]);
                org.setName(drtp[1]);
                org.setOrganizationType(regionalType);
                org.setParent(mintp);
                org.setActive(true);
                organizationRepository.save(org);
                log.info("Seed: DRTP {}", drtp[0]);
            }
        }

        seedOrganizationIfMissing("DSI", "Direction des Systèmes d'Information", directorateType, mintp);

        Organization dsi = organizationRepository.findByCode("DSI").orElseThrow();
        if (userRepository.findByEmail("e.fotso@mintp.cm").isEmpty()) {
            User admin = new User();
            admin.setStaffNumber("MAT-2014-0006");
            admin.setEmail("e.fotso@mintp.cm");
            admin.setLastName("FOTSO");
            admin.setFirstName("Emmanuel");
            admin.setPhone("+237 677 20 10 01");
            admin.setRole(UserRole.SUPER_ADMIN);
            admin.setOrganization(dsi);
            admin.setJobTitle("System administrator");
            admin.setPasswordHash(passwordEncoder.encode("Mintp@2025"));
            admin.setMustChangePassword(false);
            admin.setActive(true);
            userRepository.save(admin);
            log.info("Seed: SUPER_ADMIN e.fotso@mintp.cm / Mintp@2025");
        }
    }

    private void seedOrganizationTypes() {
        for (String[] row : TYPE_SEED) {
            UUID id = UUID.fromString(row[0]);
            String code = row[1];
            if (organizationTypeRepository.findByCode(code).isPresent()
                    || organizationTypeRepository.findById(id).isPresent()) {
                continue;
            }
            OrganizationType type = new OrganizationType();
            type.setId(id);
            type.setCode(code);
            type.setName(row[2]);
            type.setNameEn(row[3]);
            type.setColor(row[4]);
            type.setSortOrder(Integer.parseInt(row[5]));
            type.setAllowsRoot(Boolean.parseBoolean(row[6]));
            type.setRegionalScope(Boolean.parseBoolean(row[7]));
            type.setActive(true);
            organizationTypeRepository.save(type);
            log.info("Seed: organization type {}", code);
        }
    }

    private void seedOrganizationIfMissing(
            String code, String name, OrganizationType type, Organization parent) {
        if (organizationRepository.findByCode(code).isEmpty()) {
            Organization org = new Organization();
            org.setCode(code);
            org.setName(name);
            org.setOrganizationType(type);
            org.setParent(parent);
            org.setActive(true);
            organizationRepository.save(org);
        }
    }
}
