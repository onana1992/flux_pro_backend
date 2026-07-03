package com.nanotech.flux_pro_backend.mapper;

import com.nanotech.flux_pro_backend.dto.response.FileTypeResponse;
import com.nanotech.flux_pro_backend.entity.FileType;

public final class FileTypeMapper {

    private FileTypeMapper() {
    }

    public static FileTypeResponse toResponse(FileType type) {
        return new FileTypeResponse(
                type.getId(),
                type.getCode(),
                type.getName(),
                type.getNameEn(),
                type.getDescription(),
                type.getDirectionCode(),
                type.getSortOrder(),
                type.isActive());
    }
}
