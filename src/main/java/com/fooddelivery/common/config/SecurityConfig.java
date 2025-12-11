package com.fooddelivery.common.config;

import com.fooddelivery.auth.security.JwtAuthenticationEntryPoint;
import com.fooddelivery.auth.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Security configuration for the application.
 * Configures JWT-based authentication, role-based authorization,
 * and security headers.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final UserDetailsService userDetailsService;

    private static final String[] PUBLIC_ENDPOINTS = {
            "/api/v1/auth/**",
            "/api/v1/restaurants/*/menu",
            "/api/v1/restaurants/*",
            "/api/v1/webhooks/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/api-docs/**",
            "/v3/api-docs/**",
            "/actuator/health",
            "/actuator/info",
            "/actuator/prometheus",
            "/ws/**",
            "/error"
    };

    private static final String[] ADMIN_ENDPOINTS = {
            "/api/v1/admin/**"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Disable CSRF for stateless API
                .csrf(AbstractHttpConfigurer::disable)

                // Configure CORS
                .cors(cors -> cors.configurationSource(request -> {
                    var corsConfig = new org.springframework.web.cors.CorsConfiguration();
                    corsConfig.addAllowedOriginPattern("*");
                    corsConfig.addAllowedMethod("*");
                    corsConfig.addAllowedHeader("*");
                    corsConfig.setAllowCredentials(true);
                    corsConfig.setMaxAge(3600L);
                    return corsConfig;
                }))

                // Configure session management
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Configure exception handling
                .exceptionHandling(exception ->
                        exception.authenticationEntryPoint(jwtAuthenticationEntryPoint))

                // Configure authorization rules
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints
                        .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/restaurants/**").permitAll()

                        // Admin endpoints
                        .requestMatchers(ADMIN_ENDPOINTS).hasAnyRole("ADMIN", "PLATFORM")

                        // Restaurant management
                        .requestMatchers(HttpMethod.POST, "/api/v1/restaurants")
                            .hasAnyRole("RESTAURANT_OWNER", "PLATFORM", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/restaurants/**")
                            .hasAnyRole("RESTAURANT_OWNER", "RESTAURANT_STAFF", "PLATFORM", "ADMIN")
                        .requestMatchers("/api/v1/restaurants/*/menu/**")
                            .hasAnyRole("RESTAURANT_OWNER", "RESTAURANT_STAFF", "PLATFORM", "ADMIN")

                        // Order management
                        .requestMatchers(HttpMethod.POST, "/api/v1/orders").hasAnyRole("CONSUMER", "PLATFORM", "ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/orders/*/status")
                            .hasAnyRole("RESTAURANT_OWNER", "RESTAURANT_STAFF", "COURIER", "PLATFORM", "ADMIN")

                        // Courier endpoints
                        .requestMatchers("/api/v1/couriers/**").hasAnyRole("COURIER", "PLATFORM", "ADMIN")

                        // Referral endpoints
                        .requestMatchers("/api/v1/referrals/**").authenticated()

                        // Payment endpoints
                        .requestMatchers("/api/v1/payments/**").authenticated()

                        // All other requests require authentication
                        .anyRequest().authenticated()
                )

                // Add authentication provider
                .authenticationProvider(authenticationProvider())

                // Add JWT filter before UsernamePasswordAuthenticationFilter
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)

                // Security headers
                .headers(headers -> headers
                        .contentSecurityPolicy(csp ->
                                csp.policyDirectives("default-src 'self'; frame-ancestors 'none'"))
                        .frameOptions(frame -> frame.deny())
                );

        return http.build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}
