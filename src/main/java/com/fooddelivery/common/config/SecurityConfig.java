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
import org.springframework.web.cors.CorsConfigurationSource;

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
    private final CorsConfigurationSource corsConfigurationSource;

    private static final String[] PUBLIC_ENDPOINTS = {
            "/api/v1/auth/**",
            "/api/v1/restaurants/*/menu",
            "/api/v1/restaurants/*",
            "/api/v1/webhooks/**",
            "/api/v1/sms/callback/**",
            "/api/v1/images/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/api-docs/**",
            "/v3/api-docs/**",
            "/actuator/health",
            "/actuator/info",
            "/actuator/prometheus",
            "/ws/**",
            "/ws-sockjs/**",
            "/ws-native/**",
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

                // Disable form login (we use JWT)
                .formLogin(AbstractHttpConfigurer::disable)

                // CORS — single source of truth is CorsConfig, driven by the
                // app.cors.* properties. This used to build its own inline
                // configuration, which silently overrode that bean and pinned
                // allow-credentials to true regardless of configuration.
                .cors(cors -> cors.configurationSource(corsConfigurationSource))

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

                        // Restaurant financial report requires authentication (must come before permitAll)
                        .requestMatchers(HttpMethod.GET, "/api/v1/restaurants/*/financial-report")
                            .hasAnyRole("RESTAURANT_OWNER", "RESTAURANT_STAFF", "PLATFORM", "ADMIN")

                        .requestMatchers(HttpMethod.GET, "/api/v1/restaurants/**").permitAll()

                        // Admin endpoints
                        .requestMatchers(ADMIN_ENDPOINTS).hasAnyRole("ADMIN", "PLATFORM")

                        // Actuator: only health/info/prometheus are public (PUBLIC_ENDPOINTS).
                        // Everything else (env, loggers, metrics, ...) is ADMIN-only.
                        .requestMatchers("/actuator/**").hasRole("ADMIN")

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
                        //
                        // Registration MUST come before the blanket rule below, and must not
                        // demand ROLE_COURIER: this is the endpoint that GRANTS that role. The
                        // blanket rule alone made becoming a courier impossible — you needed the
                        // role to call the only thing that gives it — and because URL rules are
                        // evaluated before @PreAuthorize, the controller's hasRole('CONSUMER')
                        // never even ran. That is why the platform had courier users and zero
                        // courier profiles. Ownership is still enforced at the method level.
                        .requestMatchers(HttpMethod.POST, "/api/v1/couriers/register").authenticated()
                        // Restaurants pick a courier for an order, so they must reach this one.
                        // The method-level rule already lists RESTAURANT_OWNER; without the URL
                        // rule agreeing, they were rejected before it was consulted.
                        .requestMatchers(HttpMethod.GET, "/api/v1/couriers/available")
                            .hasAnyRole("RESTAURANT_OWNER", "PLATFORM", "ADMIN")
                        .requestMatchers("/api/v1/couriers/**").hasAnyRole("COURIER", "PLATFORM", "ADMIN")

                        // Consumer profile endpoints
                        .requestMatchers("/api/v1/consumers/profile").hasRole("CONSUMER")
                        .requestMatchers("/api/v1/consumers/**").hasAnyRole("ADMIN", "PLATFORM", "RESTAURANT_OWNER", "CONSUMER")

                        // Notification endpoints - personal notification access for all authenticated users
                        .requestMatchers("/api/v1/notifications/me").authenticated()
                        .requestMatchers("/api/v1/notifications/unread/count").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/notifications/*").authenticated()
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/notifications/*/read").authenticated()
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/notifications/*/dismiss").authenticated()
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/notifications/read-batch").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/v1/notifications/read-all").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/v1/notifications/bulk-action").authenticated()
                        // User-specific notification endpoints
                        .requestMatchers("/api/v1/notifications/user/*/unread").authenticated()
                        .requestMatchers("/api/v1/notifications/user/*/counts").authenticated()
                        .requestMatchers("/api/v1/notifications/user/*/unread-count").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/notifications/user/*").authenticated()
                        // Admin notification endpoints
                        .requestMatchers(HttpMethod.GET, "/api/v1/notifications").hasAnyRole("ADMIN", "PLATFORM")
                        .requestMatchers(HttpMethod.POST, "/api/v1/notifications").hasAnyRole("ADMIN", "SYSTEM")
                        .requestMatchers(HttpMethod.POST, "/api/v1/notifications/search").hasAnyRole("ADMIN", "PLATFORM")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/notifications/*").hasRole("ADMIN")
                        .requestMatchers("/api/v1/notifications/admin/**").hasRole("ADMIN")

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
