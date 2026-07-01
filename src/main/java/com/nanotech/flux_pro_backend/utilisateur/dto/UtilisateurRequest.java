package com.nanotech.flux_pro_backend.utilisateur.dto;

import com.nanotech.flux_pro_backend.security.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record UtilisateurRequest(
        @NotBlank String matricule,
        @NotBlank @Email String email,
        @NotBlank String nom,
        @NotBlank String prenom,
        String telephone,
        @NotNull UserRole role,
        @NotNull UUID organisationId,
        String fonction,
        boolean actif
) {
}
