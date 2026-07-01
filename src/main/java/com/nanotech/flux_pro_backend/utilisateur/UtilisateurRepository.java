package com.nanotech.flux_pro_backend.utilisateur;

import com.nanotech.flux_pro_backend.security.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UtilisateurRepository extends JpaRepository<Utilisateur, UUID> {

    Optional<Utilisateur> findByEmail(String email);

    Optional<Utilisateur> findByMatricule(String matricule);

    boolean existsByEmail(String email);

    boolean existsByMatricule(String matricule);

    @Query("""
            SELECT u FROM Utilisateur u
            WHERE (:organisationId IS NULL OR u.organisation.id = :organisationId)
              AND (:role IS NULL OR u.role = :role)
              AND (:search IS NULL OR :search = '' OR
                   LOWER(u.nom) LIKE LOWER(CONCAT('%', :search, '%')) OR
                   LOWER(u.prenom) LIKE LOWER(CONCAT('%', :search, '%')) OR
                   LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')) OR
                   LOWER(u.matricule) LIKE LOWER(CONCAT('%', :search, '%')))
            """)
    Page<Utilisateur> search(
            @Param("organisationId") UUID organisationId,
            @Param("role") UserRole role,
            @Param("search") String search,
            Pageable pageable);
}
