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
    private final com.fooddelivery.integration.partner.security.PartnerAuthenticationFilter partnerAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final UserDetailsService userDetailsService;
    private final CorsConfigurationSource corsConfigurationSource;

    private static final String[] PUBLIC_ENDPOINTS = {
            "/api/v1/auth/**",
            // Read on launch and on the login screen, before anyone has a
            // token. An update prompt gated behind sign-in cannot reach a
            // customer whose version is too old to sign in.
            "/api/v1/app/version",
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

    private final org.springframework.security.authentication.AuthenticationTrustResolver trustResolver =
            new org.springframework.security.authentication.AuthenticationTrustResolverImpl();

    /**
     * Authenticated, and not a partner.
     *
     * <p>A partner key carries authority over many restaurants, where a user
     * session is one person — so a partner reaching a general endpoint would
     * arrive somewhere written for a UserPrincipal that it is not, with an
     * ownership check that has nothing to compare against.
     *
     * <p>PartnerAuthenticationFilter already refuses to authenticate a partner
     * key outside /api/v1/partner/**, which makes this unreachable today. It is
     * here as the second gate: the courier registration deadlock in this same
     * file was a URL rule and a method rule disagreeing, and containment that
     * rests on one line in one filter is one edit away from being gone.
     */
    private org.springframework.security.authorization.AuthorizationDecision authenticatedAndNotAPartner(
            java.util.function.Supplier<org.springframework.security.core.Authentication> authentication,
            org.springframework.security.web.access.intercept.RequestAuthorizationContext context) {

        org.springframework.security.core.Authentication auth = authentication.get();
        if (auth == null || !auth.isAuthenticated() || trustResolver.isAnonymous(auth)) {
            return new org.springframework.security.authorization.AuthorizationDecision(false);
        }
        boolean partner = auth.getAuthorities().stream().anyMatch(granted ->
                com.fooddelivery.integration.partner.security.PartnerPrincipal.ROLE
                        .equals(granted.getAuthority()));
        return new org.springframework.security.authorization.AuthorizationDecision(!partner);
    }

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
                        // Self-service account deletion lives under /auth/**, which is
                        // otherwise public. Rules are evaluated in order, so this must
                        // come BEFORE the permitAll below: without it the request reaches
                        // the controller unauthenticated and @PreAuthorize answers 403,
                        // where the apps — and Apple's reviewer — expect a 401 they can
                        // tell apart from "you are not allowed to delete this".
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/auth/account").authenticated()

                        // Partner API: authenticated by X-Partner-Key, never by a
                        // user session. ROLE_PARTNER is held by nothing else, so a
                        // logged-in user cannot reach these however privileged they
                        // are — and a partner key grants nothing anywhere else.
                        .requestMatchers("/api/v1/partner/**").hasRole("PARTNER")

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

                        // All other requests require authentication — and must
                        // NOT be reachable by a partner. See
                        // authenticatedAndNotAPartner below.
                        .anyRequest().access(this::authenticatedAndNotAPartner)
                )

                // Add authentication provider
                .authenticationProvider(authenticationProvider())

                // Add JWT filter before UsernamePasswordAuthenticationFilter
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)

                // Partner keys, in their own header and only under /api/v1/partner/**.
                // Order relative to the JWT filter does not matter — they read
                // different headers and each only acts when the context is still
                // empty — but this one runs second so a request carrying both
                // credentials is treated as the user it names rather than
                // silently upgraded to a partner's reach over many restaurants.
                .addFilterAfter(partnerAuthenticationFilter, JwtAuthenticationFilter.class)

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
