package com.nanotech.flux_pro_backend.repository;

import com.nanotech.flux_pro_backend.entity.AlertDigestRecipientRole;
import com.nanotech.flux_pro_backend.enumeration.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AlertDigestRecipientRoleRepository extends JpaRepository<AlertDigestRecipientRole, UUID> {

    List<AlertDigestRecipientRole> findAllByOrderByRoleAsc();

    Optional<AlertDigestRecipientRole> findByRole(UserRole role);

    boolean existsByRole(UserRole role);

    void deleteByRole(UserRole role);
}
