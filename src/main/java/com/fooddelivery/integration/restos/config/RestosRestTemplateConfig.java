package com.fooddelivery.integration.restos.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
@RequiredArgsConstructor
public class RestosRestTemplateConfig {

    private final RestosProperties properties;

    @Bean("restosRestTemplate")
    public RestTemplate restosRestTemplate(RestTemplateBuilder builder) {
        // Disable redirect following so an attacker-hosted 302 cannot bounce the
        // request to an internal address after the SSRF validator has approved the host.
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(properties.getConnectTimeoutMs());
        factory.setReadTimeout(properties.getReadTimeoutMs());
        factory.setInstanceFollowRedirects(false);

        return builder
                .setConnectTimeout(Duration.ofMillis(properties.getConnectTimeoutMs()))
                .setReadTimeout(Duration.ofMillis(properties.getReadTimeoutMs()))
                .requestFactory(() -> factory)
                .build();
    }
}
