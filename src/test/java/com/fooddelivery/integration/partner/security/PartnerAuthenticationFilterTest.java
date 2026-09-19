package com.fooddelivery.integration.partner.security;

import com.fooddelivery.integration.partner.entity.Partner;
import com.fooddelivery.integration.partner.service.PartnerKeyService;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Where a partner key is, and is not, a credential.
 *
 * <p>This filter is the primary containment for the partner API: it refuses to
 * even look at the header outside {@code /api/v1/partner/**}, so a leaked key
 * cannot be pointed at the customer or vendor APIs. The URL rules are the
 * second gate; this is the first.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Partner authentication filter")
class PartnerAuthenticationFilterTest {

    @Mock private PartnerKeyService keyService;
    @Mock private FilterChain chain;

    private PartnerAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        filter = new PartnerAuthenticationFilter(keyService);
        SecurityContextHolder.clearContext();

        PartnerPrincipal principal = new PartnerPrincipal(
                Partner.builder().id(7L).code("RESTOS").active(true).build(), 1L);
        when(keyService.authenticate(anyString())).thenReturn(Optional.of(principal));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private MockHttpServletRequest request(String path, String key) {
        MockHttpServletRequest request = new MockHttpServletRequest("PATCH", path);
        request.setServletPath(path);
        if (key != null) {
            request.addHeader(PartnerAuthenticationFilter.HEADER, key);
        }
        return request;
    }

    private void run(MockHttpServletRequest request) throws Exception {
        if (filter.shouldNotFilter(request)) {
            chain.doFilter(request, new MockHttpServletResponse());
            return;
        }
        filter.doFilter(request, new MockHttpServletResponse(), chain);
    }

    @Test
    @DisplayName("a valid key authenticates on a partner path")
    void authenticatesOnPartnerPath() throws Exception {
        run(request("/api/v1/partner/venues/v1/menu/items/1", "zbrp_a_b"));

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal())
                .isInstanceOf(PartnerPrincipal.class);
    }

    @Test
    @DisplayName("the same key is not even looked at outside the partner API")
    void ignoredOutsidePartnerPaths() throws Exception {
        // THE containment. A leaked key pointed at the orders API is not a
        // credential there — the filter never runs, so nothing can authenticate
        // it however valid it is.
        run(request("/api/v1/orders/1", "zbrp_a_b"));

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(keyService, never()).authenticate(any());
    }

    @Test
    @DisplayName("a path that merely starts similarly is not the partner API")
    void prefixIsNotFooled() throws Exception {
        // "/api/v1/partners-report" must not match "/api/v1/partner/". The
        // trailing slash in the prefix is what makes that true, and it is one
        // character away from not being.
        run(request("/api/v1/partnerships/secret", "zbrp_a_b"));

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("a request with no key passes through unauthenticated")
    void noHeaderPassesThrough() throws Exception {
        run(request("/api/v1/partner/venues/v1/menu/items/1", null));

        // Left to the URL rules to reject, so the 401 comes from the entry
        // point with everything else's shape rather than from here.
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain).doFilter(any(), any());
    }

    @Test
    @DisplayName("a rejected key leaves the request unauthenticated but still served")
    void rejectedKeyStillContinuesTheChain() throws Exception {
        when(keyService.authenticate(anyString())).thenReturn(Optional.empty());

        run(request("/api/v1/partner/venues/v1/menu/items/1", "zbrp_bad_key"));

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        // The chain must continue so the entry point produces a proper 401.
        // Short-circuiting here would return an empty 200.
        verify(chain).doFilter(any(), any());
    }

    @Test
    @DisplayName("an existing user session is not overwritten by a partner key")
    void doesNotOverrideAnExistingAuthentication() throws Exception {
        var existing = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                "someone", null, java.util.List.of());
        SecurityContextHolder.getContext().setAuthentication(existing);

        run(request("/api/v1/partner/venues/v1/menu/items/1", "zbrp_a_b"));

        // A request carrying both credentials stays the user it names, rather
        // than being silently upgraded to a partner's reach over many venues.
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isSameAs(existing);
    }

    @Test
    @DisplayName("usage is recorded only after authentication succeeds")
    void usageRecordedAfterSuccess() throws Exception {
        run(request("/api/v1/partner/venues/v1/menu/items/1", "zbrp_a_b"));

        verify(keyService).recordUsage(1L);
    }
}
