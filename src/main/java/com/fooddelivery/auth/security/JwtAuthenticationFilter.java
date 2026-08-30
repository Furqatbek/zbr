package com.fooddelivery.auth.security;

import com.fooddelivery.auth.service.LastSeenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT Authentication Filter that intercepts every request to validate JWT tokens.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final LastSeenService lastSeenService;

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        // Skip filter for public endpoints
        if (shouldSkipAuthentication(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String jwt = extractJwtFromRequest(request);

            if (jwt == null) {
                log.debug("No JWT token found in request to: {} {}", request.getMethod(), request.getServletPath());
            } else if (SecurityContextHolder.getContext().getAuthentication() == null) {
                String userEmail = jwtService.extractUsername(jwt);

                if (userEmail != null) {
                    UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);

                    // Account state must be re-checked on EVERY request, not
                    // just at login. DaoAuthenticationProvider applies these
                    // checks when a password is exchanged for a token; this
                    // filter builds an Authentication by hand, so nothing
                    // applied them here. Without this, suspending or banning an
                    // account did not end its session: the existing access
                    // token kept working, and /auth/refresh issued fresh ones.
                    if (!isAccountUsable(userDetails)) {
                        log.warn("Rejected request from non-active account: {}", userEmail);
                    } else if (jwtService.isTokenValid(jwt, userDetails) && jwtService.isAccessToken(jwt)) {
                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(
                                        userDetails,
                                        null,
                                        userDetails.getAuthorities()
                                );

                        authentication.setDetails(
                                new WebAuthenticationDetailsSource().buildDetails(request)
                        );

                        SecurityContextHolder.getContext().setAuthentication(authentication);
                        log.debug("Authenticated user: {} with roles: {}", userEmail, userDetails.getAuthorities());

                        // Last-online. Here rather than in an interceptor so it
                        // counts exactly what it claims to: a request that
                        // actually authenticated. Internally throttled, so this
                        // is not a write per request.
                        if (userDetails instanceof UserPrincipal p) {
                            lastSeenService.touch(p.getId());
                        }
                    } else {
                        log.debug("Token validation failed for user: {}, isValid: {}, isAccessToken: {}",
                                userEmail, jwtService.isTokenValid(jwt, userDetails), jwtService.isAccessToken(jwt));
                    }
                } else {
                    log.debug("Could not extract username from JWT token");
                }
            }
        } catch (Exception e) {
            log.warn("Could not set user authentication for {} {}: {}",
                    request.getMethod(), request.getServletPath(), e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Whether the loaded account may still act. Maps to UserPrincipal:
     * enabled = ACTIVE, non-locked = not SUSPENDED/BANNED, non-expired =
     * not DELETED.
     */
    private boolean isAccountUsable(UserDetails userDetails) {
        return userDetails.isEnabled()
                && userDetails.isAccountNonLocked()
                && userDetails.isAccountNonExpired();
    }

    private String extractJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);

        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }

        return null;
    }

    private boolean shouldSkipAuthentication(HttpServletRequest request) {
        String path = request.getServletPath();

        // /api/v1/auth/me requires authentication - don't skip
        if (path.equals("/api/v1/auth/me")) {
            return false;
        }

        // Skip authentication for these paths
        return path.startsWith("/api/v1/auth/") ||
                path.startsWith("/swagger-ui") ||
                path.startsWith("/api-docs") ||
                path.startsWith("/v3/api-docs") ||
                path.startsWith("/actuator/health") ||
                path.startsWith("/actuator/info") ||
                path.startsWith("/actuator/prometheus") ||
                path.startsWith("/ws") ||
                path.startsWith("/api/v1/webhooks/");
    }
}
