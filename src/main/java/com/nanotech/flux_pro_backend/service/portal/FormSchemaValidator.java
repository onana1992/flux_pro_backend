package com.nanotech.flux_pro_backend.service.portal;

import com.nanotech.flux_pro_backend.common.AppException;
import com.nanotech.flux_pro_backend.enumeration.PortalAudience;
import com.nanotech.flux_pro_backend.enumeration.PortalUserType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Validation serveur du {@code formData} contre le schéma de formulaire d'un dossier préconfiguré.
 */
@Component
public class FormSchemaValidator {

    private static final Set<String> SUPPORTED_TYPES = Set.of(
            "TEXT", "TEXTAREA", "NUMBER", "DATE", "DATETIME", "ENUM", "MULTI_ENUM", "BOOLEAN", "EMAIL", "PHONE");

    @SuppressWarnings("unchecked")
    public Map<String, Object> validate(Map<String, Object> formSchema, Map<String, Object> formData) {
        if (formSchema == null || formSchema.isEmpty()) {
            throw AppException.badRequest("PORTAL_FORM_SCHEMA_MISSING", "Portal form schema is missing");
        }
        Object fieldsRaw = formSchema.get("fields");
        if (!(fieldsRaw instanceof List<?> fields) || fields.isEmpty()) {
            throw AppException.badRequest("PORTAL_FORM_SCHEMA_INVALID", "Form schema must declare fields");
        }

        Map<String, Object> data = formData != null ? formData : Map.of();
        Map<String, Object> validated = new LinkedHashMap<>();

        for (Object fieldObj : fields) {
            if (!(fieldObj instanceof Map<?, ?> fieldMap)) {
                throw AppException.badRequest("PORTAL_FORM_SCHEMA_INVALID", "Invalid field definition");
            }
            Map<String, Object> field = (Map<String, Object>) fieldMap;
            String key = stringVal(field.get("key"));
            if (key == null || key.isBlank()) {
                throw AppException.badRequest("PORTAL_FORM_SCHEMA_INVALID", "Field key is required");
            }
            String type = stringVal(field.get("type"));
            if (type == null) {
                throw AppException.badRequest("PORTAL_FORM_SCHEMA_INVALID", "Field type is required: " + key, key);
            }
            type = type.trim().toUpperCase(Locale.ROOT);
            if (!SUPPORTED_TYPES.contains(type)) {
                throw AppException.badRequest(
                        "PORTAL_FORM_FIELD_TYPE_UNSUPPORTED", "Unsupported field type: " + type, type);
            }

            boolean required = Boolean.TRUE.equals(field.get("required"))
                    || "true".equalsIgnoreCase(stringVal(field.get("required")));
            Object raw = data.get(key);
            boolean missing = raw == null
                    || (raw instanceof String s && s.isBlank())
                    || (raw instanceof Collection<?> c && c.isEmpty());

            if (missing) {
                if (required) {
                    throw AppException.badRequest(
                            "PORTAL_FORM_FIELD_REQUIRED", "Required field missing: " + key, key);
                }
                continue;
            }

            validated.put(key, coerceAndValidate(key, type, raw, field));
        }
        return validated;
    }

    public void assertAudienceCompatible(PortalUserType userType, PortalAudience audience) {
        if (audience == null) {
            throw AppException.forbidden(
                    "PORTAL_AUDIENCE_UNDEFINED", "Portal audience is not configured for this dossier");
        }
        if (audience == PortalAudience.BOTH) {
            return;
        }
        boolean ok = switch (userType) {
            case INTERNAL_EMPLOYEE -> audience == PortalAudience.INTERNAL;
            case EXTERNAL -> audience == PortalAudience.EXTERNAL;
        };
        if (!ok) {
            throw AppException.forbidden(
                    "PORTAL_AUDIENCE_MISMATCH",
                    "Dossier audience is not compatible with portal user type");
        }
    }

    @SuppressWarnings("unchecked")
    private Object coerceAndValidate(String key, String type, Object raw, Map<String, Object> field) {
        return switch (type) {
            case "TEXT", "TEXTAREA", "EMAIL", "PHONE" -> {
                String value = String.valueOf(raw).trim();
                Integer maxLength = intVal(field.get("maxLength"));
                if (maxLength != null && value.length() > maxLength) {
                    throw AppException.badRequest(
                            "PORTAL_FORM_FIELD_TOO_LONG", "Field exceeds maxLength: " + key, key);
                }
                if ("EMAIL".equals(type) && !value.contains("@")) {
                    throw AppException.badRequest(
                            "PORTAL_FORM_FIELD_EMAIL_INVALID", "Invalid email: " + key, key);
                }
                yield value;
            }
            case "NUMBER" -> {
                try {
                    if (raw instanceof Number n) {
                        yield n;
                    }
                    yield new BigDecimal(String.valueOf(raw).trim());
                } catch (NumberFormatException e) {
                    throw AppException.badRequest(
                            "PORTAL_FORM_FIELD_NUMBER_INVALID", "Invalid number: " + key, key);
                }
            }
            case "DATE" -> {
                try {
                    // Stocker en ISO string (JSON-friendly) — pas LocalDate
                    yield LocalDate.parse(String.valueOf(raw).trim()).toString();
                } catch (DateTimeParseException e) {
                    throw AppException.badRequest(
                            "PORTAL_FORM_FIELD_DATE_INVALID", "Invalid date (ISO): " + key, key);
                }
            }
            case "DATETIME" -> {
                try {
                    String s = String.valueOf(raw).trim();
                    LocalDateTime dt;
                    if (s.endsWith("Z") || s.contains("+")) {
                        dt = LocalDateTime.parse(s.substring(0, Math.min(s.length(), 19)));
                    } else {
                        dt = LocalDateTime.parse(s);
                    }
                    yield dt.toString();
                } catch (DateTimeParseException e) {
                    throw AppException.badRequest(
                            "PORTAL_FORM_FIELD_DATETIME_INVALID", "Invalid datetime: " + key, key);
                }
            }
            case "BOOLEAN" -> {
                if (raw instanceof Boolean b) {
                    yield b;
                }
                String s = String.valueOf(raw).trim().toLowerCase(Locale.ROOT);
                if ("true".equals(s) || "false".equals(s)) {
                    yield Boolean.parseBoolean(s);
                }
                throw AppException.badRequest(
                        "PORTAL_FORM_FIELD_BOOLEAN_INVALID", "Invalid boolean: " + key, key);
            }
            case "ENUM" -> {
                String value = String.valueOf(raw).trim();
                List<String> options = optionsOf(field);
                if (!options.contains(value)) {
                    throw AppException.badRequest(
                            "PORTAL_FORM_FIELD_ENUM_INVALID", "Value not in options: " + key, key);
                }
                yield value;
            }
            case "MULTI_ENUM" -> {
                List<String> values = new ArrayList<>();
                if (raw instanceof Collection<?> col) {
                    for (Object o : col) {
                        values.add(String.valueOf(o).trim());
                    }
                } else {
                    values.add(String.valueOf(raw).trim());
                }
                List<String> options = optionsOf(field);
                for (String v : values) {
                    if (!options.contains(v)) {
                        throw AppException.badRequest(
                                "PORTAL_FORM_FIELD_ENUM_INVALID", "Value not in options: " + key, key);
                    }
                }
                yield values;
            }
            default -> throw AppException.badRequest(
                    "PORTAL_FORM_FIELD_TYPE_UNSUPPORTED", "Unsupported field type: " + type, type);
        };
    }

    @SuppressWarnings("unchecked")
    private List<String> optionsOf(Map<String, Object> field) {
        Object options = field.get("options");
        if (!(options instanceof List<?> list) || list.isEmpty()) {
            throw AppException.badRequest("PORTAL_FORM_SCHEMA_INVALID", "ENUM field requires options");
        }
        List<String> result = new ArrayList<>();
        for (Object o : list) {
            result.add(String.valueOf(o));
        }
        return result;
    }

    private String stringVal(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    private Integer intVal(Object o) {
        if (o == null) {
            return null;
        }
        if (o instanceof Number n) {
            return n.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(o).trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
