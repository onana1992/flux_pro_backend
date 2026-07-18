package com.nanotech.flux_pro_backend.repository;

import com.nanotech.flux_pro_backend.entity.FileNumberSequence;
import com.nanotech.flux_pro_backend.entity.FileNumberSequenceId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface FileNumberSequenceRepository extends JpaRepository<FileNumberSequence, FileNumberSequenceId> {

    /**
     * Verrouillage ligne pour allocation séquentielle (MariaDB 10.4 : pas de {@code FOR UPDATE OF alias}).
     */
    @Query(
            value = """
                    SELECT organization_id, year, last_sequence
                    FROM file_number_sequences
                    WHERE organization_id = :orgId AND year = :year
                    FOR UPDATE
                    """,
            nativeQuery = true)
    Optional<FileNumberSequence> findForUpdate(@Param("orgId") UUID organizationId, @Param("year") int year);

    /** Nettoyage technique avant suppression d'une organisation sans dossiers. */
    @Modifying
    @Query("DELETE FROM FileNumberSequence s WHERE s.organizationId = :organizationId")
    void deleteByOrganizationId(@Param("organizationId") UUID organizationId);
}
