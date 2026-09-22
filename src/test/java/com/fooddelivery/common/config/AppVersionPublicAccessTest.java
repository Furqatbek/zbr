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
 * The version check has to work before anyone has a token.
 *
 * <p>A controller test cannot see this: the URL rules run first, and a
 * @PreAuthorize that never executes proves nothing. If this endpoint were
 * authenticated, a customer whose app is too old to sign in would never be told
 * to update — the one case the prompt exists for.
 */
@ExtendWith(SpringExtension.class)
@WebMvcTest(controllers = AppVersionPublicAccessTest.ProbeController.class)
@Import({SecurityConfig.class, AppVersionPublicAccessTest.ProbeController.class,
        AppVersionPublicAccessTest.CorsStub.class})
@DisplayName("App version endpoint access")
class AppVersionPublicAccessTest {

    @org.springframework.boot.SpringBootConfiguration
    @org.springframework.boot.autoconfigure.EnableAutoConfiguration
    static class TestApp {
    }

    /** Reaching it proves the URL rules allowed an anonymous request through. */
    @org.springframework.web.bind.annotation.RestController
    static class ProbeController {

        @org.springframework.web.bind.annotation.GetMapping("/api/v1/app/version")
        String version() {
            return "reached";
        }

        @org.springframework.web.bind.annotation.GetMapping("/api/v1/app/config")
        String somethingElseUnderApp() {
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
    @DisplayName("anyone can ask, with no token")
    void versionIsPublic() throws Exception {
        mockMvc.perform(get("/api/v1/app/version").param("platform", "android"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("the exemption covers that one path, not everything under /app")
    void theExemptionIsNarrow() throws Exception {
        // A wildcard here would quietly make any future /api/v1/app/** endpoint
        // public the moment it was written.
        mockMvc.perform(get("/api/v1/app/config"))
                .andExpect(status().isUnauthorized());
    }
}
