package com.nanotech.flux_pro_backend.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nanotech.flux_pro_backend.common.AppException;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.http.HttpStatus;

import java.util.Collections;
import java.util.List;

@Converter
public class JsonStringListConverter implements AttributeConverter<List<String>, String> {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<List<String>> LIST_TYPE = new TypeReference<>() {};

    @Override
    public String convertToDatabaseColumn(List<String> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return null;
        }
        try {
            return MAPPER.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            throw new AppException(
                    HttpStatus.BAD_REQUEST,
                    "JSON_LIST_SERIALIZE_FAILED",
                    "Cannot serialize JSON string list",
                    e);
        }
    }

    @Override
    public List<String> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return Collections.emptyList();
        }
        try {
            List<String> list = MAPPER.readValue(dbData, LIST_TYPE);
            return list != null ? list : Collections.emptyList();
        } catch (JsonProcessingException e) {
            throw new AppException(
                    HttpStatus.BAD_REQUEST,
                    "JSON_LIST_DESERIALIZE_FAILED",
                    "Cannot deserialize JSON string list",
                    e);
        }
    }
}
