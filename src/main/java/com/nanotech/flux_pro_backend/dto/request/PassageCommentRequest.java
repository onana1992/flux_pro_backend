package com.nanotech.flux_pro_backend.dto.request;

import jakarta.validation.constraints.Size;

public record PassageCommentRequest(
        @Size(max = 2000) String internalComment
) {
}
