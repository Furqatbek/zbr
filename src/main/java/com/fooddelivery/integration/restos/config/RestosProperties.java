package com.fooddelivery.integration.restos.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app.integration.restos")
@Data
public class RestosProperties {
    private boolean enabled = true;
    private int connectTimeoutMs = 5000;
    private int readTimeoutMs = 10000;
}
