package com.fooddelivery.order.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * Configuration for the routing REST client used by RouteDistanceService.
 */
@Configuration
@RequiredArgsConstructor
public class RoutingRestTemplateConfig {

    private final RoutingProperties properties;

    @Bean("routingRestTemplate")
    public RestTemplate routingRestTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofMillis(properties.getConnectTimeout()))
                .setReadTimeout(Duration.ofMillis(properties.getReadTimeout()))
                .build();
    }
}
