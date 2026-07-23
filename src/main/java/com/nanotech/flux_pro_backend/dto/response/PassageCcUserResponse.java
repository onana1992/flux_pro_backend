package com.nanotech.flux_pro_backend.dto.response;

import java.util.UUID;

/** Utilisateur en copie informée (CHN-09) sur un maillon. */
public record PassageCcUserResponse(
        UUID id,
        String firstName,
        String lastName,
        String email
) {
}
