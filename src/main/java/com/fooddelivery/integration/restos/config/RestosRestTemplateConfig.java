package com.fooddelivery.integration.restos.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.time.Duration;

@Configuration
@RequiredArgsConstructor
public class RestosRestTemplateConfig {

    private final RestosProperties properties;

    @Bean("restosRestTemplate")
    public RestTemplate restosRestTemplate(RestTemplateBuilder builder) {
        // Disable redirect following so an attacker-hosted 302 cannot bounce the
        // request to an internal address after the SSRF validator has approved the host.
        // SimpleClientHttpRequestFactory has no direct setter for this, so we disable
        // it on the underlying HttpURLConnection via the prepareConnection hook.
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory() {
            @Override
            protected void prepareConnection(HttpURLConnection connection, String httpMethod) throws IOException {
                super.prepareConnection(connection, httpMethod);
                connection.setInstanceFollowRedirects(false);
            }
        };
        factory.setConnectTimeout(properties.getConnectTimeoutMs());
        factory.setReadTimeout(properties.getReadTimeoutMs());

        return builder
                .setConnectTimeout(Duration.ofMillis(properties.getConnectTimeoutMs()))
                .setReadTimeout(Duration.ofMillis(properties.getReadTimeoutMs()))
                .requestFactory(() -> factory)
                .build();
    }
}
