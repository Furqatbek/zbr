package com.fooddelivery.common.config;

import com.fooddelivery.auth.security.JwtAuthenticationEntryPoint;
import com.fooddelivery.auth.security.JwtAuthenticationFilter;
import com.fooddelivery.integration.partner.security.PartnerAuthenticationFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.cors.CorsConfigurationSource;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The QR landing endpoint is open, and nothing else under /public is.
 *
 * <p>It has to be reachable with no token: it is scanned from a poster by
 * someone who has no account and no app. But "public" is an inviting prefix,
 * and the first version of this rule was {@code /api/v1/public/**} — which
 * would have made every future controller under it anonymous the moment
 * somebody created one, without anyone deciding that it should be.
 *
 * <p>A controller test cannot see any of this. The URL rules run first, and a
 * {@code @PreAuthorize} that never executes proves nothing.
 */
@ExtendWith(SpringExtension.class)
@WebMvcTest(controllers = PublicLandingAccessTest.ProbeController.class)
@Import({SecurityConfig.class, PublicLandingAccessTest.ProbeController.class,
        PublicLandingAccessTest.CorsStub.class})
@DisplayName("Public landing endpoint access")
class PublicLandingAccessTest {

    @org.springframework.boot.SpringBootConfiguration
    @org.springframework.boot.autoconfigure.EnableAutoConfiguration
    static class TestApp {
    }

    /** Reaching one proves the URL rules let an anonymous request through. */
    @org.springframework.web.bind.annotation.RestController
    static class ProbeController {

        @org.springframework.web.bind.annotation.GetMapping("/api/v1/public/r/{slugOrId}")
        String landing(@org.springframework.web.bind.annotation.PathVariable String slugOrId) {
            return "reached";
        }

        @org.springframework.web.bind.annotation.GetMapping("/api/v1/public/admin-ish")
        String somethingElseUnderPublic() {
            return "reached";
        }

        @org.springframework.web.bind.annotation.GetMapping("/api/v1/public/r/{slugOrId}/orders")
        String deeperUnderR(@org.springframework.web.bind.annotation.PathVariable String slugOrId) {
            return "reached";
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @MockBean private JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockBean private PartnerAuthenticationFilter partnerAuthenticationFilter;
    @MockBean private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    @MockBean private UserDetailsService userDetailsService;

    @org.springframework.boot.test.context.TestConfiguration
    static class CorsStub {
        @org.springframework.context.annotation.Bean
        CorsConfigurationSource corsConfigurationSource() {
            return request -> new org.springframework.web.cors.CorsConfiguration();
        }
    }

    @BeforeEach
    void chainPassesThrough() throws Exception {
        org.mockito.Mockito.doAnswer(invocation -> {
            jakarta.servlet.FilterChain chain = invocation.getArgument(2);
            chain.doFilter(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(jwtAuthenticationFilter).doFilter(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());

        org.mockito.Mockito.doAnswer(invocation -> {
            jakarta.servlet.FilterChain chain = invocation.getArgument(2);
            chain.doFilter(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(partnerAuthenticationFilter).doFilter(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());

        org.mockito.Mockito.doAnswer(invocation -> {
            ((jakarta.servlet.http.HttpServletResponse) invocation.getArgument(1))
                    .sendError(jakarta.servlet.http.HttpServletResponse.SC_UNAUTHORIZED);
            return null;
        }).when(jwtAuthenticationEntryPoint).commence(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("a scanned code resolves with no token")
    void landingIsPublic() throws Exception {
        mockMvc.perform(get("/api/v1/public/r/qahvoon")).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/public/r/3")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("the rest of /public is not public")
    void siblingUnderPublicIsNot() throws Exception {
        // The whole point of naming the path rather than wildcarding the
        // prefix: this is what a future handler put in the obvious-looking
        // place would inherit.
        mockMvc.perform(get("/api/v1/public/admin-ish")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("nor is anything nested below a restaurant code")
    void deeperPathIsNot() throws Exception {
        mockMvc.perform(get("/api/v1/public/r/qahvoon/orders")).andExpect(status().isUnauthorized());
    }
}
