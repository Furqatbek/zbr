package com.fooddelivery.common.time;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.time.ZoneId;

/**
 * Binds {@code app.timezone} into {@link BusinessTime}.
 *
 * <p>BusinessTime is static because the calendar helpers are needed from request
 * DTOs and static utility classes that Spring does not manage. Configuring it
 * once here keeps the zone in one place rather than scattering
 * {@code ZoneId.of(...)} through the codebase.
 */
@Configuration
@Slf4j
public class BusinessTimeConfig {

    public BusinessTimeConfig(@Value("${app.timezone:Asia/Tashkent}") String timezone) {
        ZoneId zone = ZoneId.of(timezone);
        BusinessTime.configure(zone);
        log.info("Business timezone set to {} (storage remains UTC)", zone);
    }
}
