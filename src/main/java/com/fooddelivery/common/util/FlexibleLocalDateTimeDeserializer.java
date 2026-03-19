package com.fooddelivery.common.util;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Flexible deserializer for LocalDateTime that accepts both date-only (yyyy-MM-dd)
 * and full datetime (yyyy-MM-dd'T'HH:mm:ss) formats.
 * Date-only values are converted to start of day (00:00:00).
 */
public class FlexibleLocalDateTimeDeserializer extends JsonDeserializer<LocalDateTime> {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    @Override
    public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String value = p.getValueAsString();
        if (value == null || value.isEmpty()) {
            return null;
        }

        // Try parsing as full datetime first
        try {
            return LocalDateTime.parse(value, DATETIME_FORMATTER);
        } catch (DateTimeParseException e) {
            // Fall through to try date-only format
        }

        // Try parsing as date-only and convert to start of day
        try {
            LocalDate date = LocalDate.parse(value, DATE_FORMATTER);
            return date.atStartOfDay();
        } catch (DateTimeParseException e) {
            throw new IOException("Cannot parse date/datetime: " + value +
                    ". Expected format: yyyy-MM-dd or yyyy-MM-dd'T'HH:mm:ss", e);
        }
    }
}
