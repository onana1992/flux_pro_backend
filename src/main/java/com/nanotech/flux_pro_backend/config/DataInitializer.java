package com.nanotech.flux_pro_backend.config;

import com.nanotech.flux_pro_backend.entity.Organization;
import com.nanotech.flux_pro_backend.entity.User;
import com.nanotech.flux_pro_backend.enumeration.OrganizationType;
import com.nanotech.flux_pro_backend.enumeration.UserRole;
import com.nanotech.flux_pro_backend.repository.OrganizationRepository;
import com.nanotech.flux_pro_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

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
        Organization mintp = organizationRepository.findByCode("MINTP").orElseGet(() -> {
            Organization root = new Organization();
            root.setCode("MINTP");
            root.setName("Ministère des Travaux Publics");
            root.setType(OrganizationType.MINISTRY);
            root.setActive(true);
            log.info("Seed: created MINTP organization");
            return organizationRepository.save(root);
        });

        for (String[] drtp : DRTP_SEED) {
            if (organizationRepository.findByCode(drtp[0]).isEmpty()) {
                Organization org = new Organization();
                org.setCode(drtp[0]);
                org.setName(drtp[1]);
                org.setType(OrganizationType.REGIONAL_DIRECTORATE);
                org.setParent(mintp);
                org.setActive(true);
                organizationRepository.save(org);
                log.info("Seed: DRTP {}", drtp[0]);
            }
        }

        seedOrganizationIfMissing("DSI", "Direction des Systèmes d'Information", OrganizationType.DIRECTORATE, mintp);

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

    private void seedOrganizationIfMissing(String code, String name, OrganizationType type, Organization parent) {
        if (organizationRepository.findByCode(code).isEmpty()) {
            Organization org = new Organization();
            org.setCode(code);
            org.setName(name);
            org.setType(type);
            org.setParent(parent);
            org.setActive(true);
            organizationRepository.save(org);
        }
    }
}
