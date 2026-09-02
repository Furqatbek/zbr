package com.fooddelivery.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The exact wire format of a timestamp, pinned.
 *
 * <p>Three documents and a javadoc disagreed about whether timestamps carry a
 * trailing 'Z', and the mobile teams had to guess. A client that guesses wrong
 * shifts every displayed time by five hours in Tashkent, silently — no error,
 * just wrong times on every order. This test is the answer, and it fails if
 * anyone changes the format without meaning to.
 */
@DisplayName("Timestamp wire format")
class JacksonTimestampFormatTest {

    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        Jackson2ObjectMapperBuilder builder = new Jackson2ObjectMapperBuilder();
        new JacksonConfig().localDateTimeUtcSerializer().customize(builder);
        mapper = builder.build();
    }

    record Payload(LocalDateTime createdAt) {
    }

    @Test
    @DisplayName("a LocalDateTime serialises with a trailing Z and no fractional seconds")
    void serialisesWithTrailingZ() throws Exception {
        String json = mapper.writeValueAsString(
                new Payload(LocalDateTime.of(2026, 9, 1, 13, 6, 32, 895_000_000)));

        assertThat(json).isEqualTo("{\"createdAt\":\"2026-09-01T13:06:32Z\"}");
    }

    @Test
    @DisplayName("milliseconds are dropped, not rounded")
    void truncatesSubSecondPrecision() throws Exception {
        // Worth stating explicitly: clients must not expect millisecond
        // precision, and must not use these values to order events that can
        // occur within the same second.
        String json = mapper.writeValueAsString(
                new Payload(LocalDateTime.of(2026, 9, 1, 13, 6, 32, 999_000_000)));

        assertThat(json).contains("13:06:32Z").doesNotContain(".999");
    }

    @Test
    @DisplayName("midnight still carries the full time, not a bare date")
    void midnightIsNotAbbreviated() throws Exception {
        String json = mapper.writeValueAsString(
                new Payload(LocalDateTime.of(2026, 9, 1, 0, 0, 0)));

        assertThat(json).isEqualTo("{\"createdAt\":\"2026-09-01T00:00:00Z\"}");
    }
}
