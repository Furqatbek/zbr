package com.fooddelivery.common.config;

import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Serialize every {@link LocalDateTime} as UTC ISO-8601 WITH a trailing 'Z'.
 *
 * The entities store instants as zone-less LocalDateTime but the database and
 * Hibernate are pinned to UTC (spring.datasource TimeZone=UTC,
 * hibernate.jdbc.time_zone=UTC), so the values ARE UTC. Without the 'Z' the wire
 * format (e.g. "2026-07-05T14:30:00") is ambiguous and mobile clients apply the
 * device timezone, shifting every timestamp by the local offset (+5h in Tashkent).
 * Appending 'Z' makes the values unambiguously UTC.
 */
@Configuration
public class JacksonConfig {

    private static final DateTimeFormatter UTC_ISO =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer localDateTimeUtcSerializer() {
        return builder -> builder.serializers(new LocalDateTimeSerializer(UTC_ISO));
    }
}
