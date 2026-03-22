package com.fooddelivery.common.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.multipart.support.MultipartFilter;

/**
 * Configuration to ensure MultipartFilter runs before Spring Security.
 * This is necessary for multipart file uploads to work correctly with JWT authentication.
 */
@Configuration
public class MultipartFilterConfig {

    /**
     * Register MultipartFilter with high precedence to ensure it runs before Spring Security.
     * This allows the Authorization header to be accessible during multipart request processing.
     */
    @Bean
    public FilterRegistrationBean<MultipartFilter> multipartFilterRegistration() {
        FilterRegistrationBean<MultipartFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new MultipartFilter());
        registration.addUrlPatterns("/*");
        // Set order before Spring Security filter (-100 is before security filter's default)
        registration.setOrder(-100);
        registration.setName("multipartFilter");
        return registration;
    }
}
