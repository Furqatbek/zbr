package com.fooddelivery.integration.partner.security;

import com.fooddelivery.integration.partner.service.PartnerKeyService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Authenticates partner requests from the {@code X-Partner-Key} header.
 *
 * <p><strong>Why not {@code Authorization: Bearer}.</strong> That header is
 * already consumed by {@code JwtAuthenticationFilter}, which would try to parse
 * a partner key as a JWT, fail, and leave the request unauthenticated with a
 * misleading message. Keeping the two credential types in separate headers means
 * neither filter has to guess which kind it is looking at, and a partner key can
 * never be mistaken for a user session — which matters, because a user session
 * carries ownership over exactly one account while a partner key carries
 * authority over many restaurants.
 *
 * <p>Only runs for {@code /api/v1/partner/**}. Everywhere else a partner key is
 * simply not a credential, so a leaked one cannot be used against the customer
 * or vendor APIs.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PartnerAuthenticationFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Partner-Key";
    public static final String PATH_PREFIX = "/api/v1/partner/";

    private final PartnerKeyService partnerKeyService;

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        return !request.getServletPath().startsWith(PATH_PREFIX);
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String presented = request.getHeader(HEADER);
        if (presented != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            partnerKeyService.authenticate(presented).ifPresentOrElse(
                    principal -> {
                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(
                                        principal, null, principal.getAuthorities());
                        authentication.setDetails(
                                new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authentication);

                        // After the context is set, so a failure here cannot
                        // cost the partner their authentication.
                        partnerKeyService.recordUsage(principal.getKeyId());
                    },
                    () -> log.warn("Rejected partner key on {} {}",
                            request.getMethod(), request.getServletPath()));
        }

        filterChain.doFilter(request, response);
    }
}
