package com.nanotech.flux_pro_backend.common;

import java.util.List;

public record ImportResult(int created, int updated, List<String> errors) {
}
