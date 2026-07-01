package com.nanotech.flux_pro_backend.config;

import com.nanotech.flux_pro_backend.organisation.Organisation;
import com.nanotech.flux_pro_backend.organisation.OrganisationRepository;
import com.nanotech.flux_pro_backend.organisation.OrganisationType;
import com.nanotech.flux_pro_backend.security.UserRole;
import com.nanotech.flux_pro_backend.utilisateur.Utilisateur;
import com.nanotech.flux_pro_backend.utilisateur.UtilisateurRepository;
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

    private final OrganisationRepository organisationRepository;
    private final UtilisateurRepository utilisateurRepository;
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
        Organisation mintp = organisationRepository.findByCode("MINTP").orElseGet(() -> {
            Organisation root = new Organisation();
            root.setCode("MINTP");
            root.setNom("Ministère des Travaux Publics");
            root.setType(OrganisationType.MINISTERE);
            root.setActif(true);
            log.info("Seed: création organisation MINTP");
            return organisationRepository.save(root);
        });

        for (String[] drtp : DRTP_SEED) {
            if (organisationRepository.findByCode(drtp[0]).isEmpty()) {
                Organisation org = new Organisation();
                org.setCode(drtp[0]);
                org.setNom(drtp[1]);
                org.setType(OrganisationType.DRTP);
                org.setParent(mintp);
                org.setActif(true);
                organisationRepository.save(org);
                log.info("Seed: DRTP {}", drtp[0]);
            }
        }

        seedOrganisationIfMissing("DSI", "Direction des Systèmes d'Information", OrganisationType.DIRECTION, mintp);

        Organisation dsi = organisationRepository.findByCode("DSI").orElseThrow();
        if (utilisateurRepository.findByEmail("e.fotso@mintp.cm").isEmpty()) {
            Utilisateur admin = new Utilisateur();
            admin.setMatricule("MAT-2014-0006");
            admin.setEmail("e.fotso@mintp.cm");
            admin.setNom("FOTSO");
            admin.setPrenom("Emmanuel");
            admin.setTelephone("+237 677 20 10 01");
            admin.setRole(UserRole.SUPER_ADMIN);
            admin.setOrganisation(dsi);
            admin.setFonction("Administrateur système");
            admin.setPasswordHash(passwordEncoder.encode("Mintp@2025"));
            admin.setMustChangePassword(false);
            admin.setActif(true);
            utilisateurRepository.save(admin);
            log.info("Seed: SUPER_ADMIN e.fotso@mintp.cm / Mintp@2025");
        }
    }

    private void seedOrganisationIfMissing(String code, String nom, OrganisationType type, Organisation parent) {
        if (organisationRepository.findByCode(code).isEmpty()) {
            Organisation org = new Organisation();
            org.setCode(code);
            org.setNom(nom);
            org.setType(type);
            org.setParent(parent);
            org.setActif(true);
            organisationRepository.save(org);
        }
    }
}
