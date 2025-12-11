package com.fooddelivery.common.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Utility class for JSON operations.
 */
@Slf4j
public final class JsonUtils {

    private static final ObjectMapper objectMapper;

    static {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    private JsonUtils() {
        // Private constructor to prevent instantiation
    }

    /**
     * Convert object to JSON string.
     */
    public static String toJson(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            log.error("Error converting object to JSON: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Convert object to pretty-printed JSON string.
     */
    public static String toPrettyJson(Object object) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(object);
        } catch (JsonProcessingException e) {
            log.error("Error converting object to pretty JSON: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Convert JSON string to object.
     */
    public static <T> T fromJson(String json, Class<T> clazz) {
        try {
            return objectMapper.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            log.error("Error parsing JSON to {}: {}", clazz.getSimpleName(), e.getMessage());
            return null;
        }
    }

    /**
     * Convert JSON string to object with TypeReference (for generic types).
     */
    public static <T> T fromJson(String json, TypeReference<T> typeReference) {
        try {
            return objectMapper.readValue(json, typeReference);
        } catch (JsonProcessingException e) {
            log.error("Error parsing JSON: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Convert JSON string to List.
     */
    public static <T> List<T> fromJsonToList(String json, Class<T> elementClass) {
        try {
            return objectMapper.readValue(json,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, elementClass));
        } catch (JsonProcessingException e) {
            log.error("Error parsing JSON to List<{}>: {}", elementClass.getSimpleName(), e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Convert JSON string to Map.
     */
    public static Map<String, Object> fromJsonToMap(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (JsonProcessingException e) {
            log.error("Error parsing JSON to Map: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }

    /**
     * Convert object to Map.
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> objectToMap(Object object) {
        return objectMapper.convertValue(object, Map.class);
    }

    /**
     * Convert Map to object.
     */
    public static <T> T mapToObject(Map<String, Object> map, Class<T> clazz) {
        return objectMapper.convertValue(map, clazz);
    }

    /**
     * Parse JSON string to JsonNode for dynamic access.
     */
    public static Optional<JsonNode> parseJson(String json) {
        try {
            return Optional.of(objectMapper.readTree(json));
        } catch (JsonProcessingException e) {
            log.error("Error parsing JSON: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Check if a string is valid JSON.
     */
    public static boolean isValidJson(String json) {
        if (json == null || json.isBlank()) {
            return false;
        }
        try {
            objectMapper.readTree(json);
            return true;
        } catch (JsonProcessingException e) {
            return false;
        }
    }

    /**
     * Get the shared ObjectMapper instance.
     */
    public static ObjectMapper getObjectMapper() {
        return objectMapper;
    }
}
