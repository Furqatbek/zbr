package com.fooddelivery.common.config;

import com.fooddelivery.auth.security.JwtAuthenticationEntryPoint;
import com.fooddelivery.auth.security.JwtAuthenticationFilter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.cors.CorsConfigurationSource;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@code DELETE /api/v1/auth/account} sits under {@code /api/v1/auth/**}, which
 * is public so that login and registration work without a token. Rules are
 * evaluated in declaration order, so the explicit rule for this one path has to
 * stay ahead of that permitAll — otherwise an anonymous DELETE reaches the
 * controller, {@code @PreAuthorize} rejects it as a 403, and the apps cannot
 * tell "sign in again" apart from "you may not do this".
 *
 * <p>Apple rejects an app whose in-app deletion does not work, so this path
 * failing is a store blocker rather than a bug report.
 */
@ExtendWith(SpringExtension.class)
@WebMvcTest(controllers = AccountDeletionAuthorizationTest.ProbeController.class)
@Import({SecurityConfig.class, AccountDeletionAuthorizationTest.ProbeController.class,
        AccountDeletionAuthorizationTest.CorsStub.class})
@DisplayName("Account deletion authorization")
class AccountDeletionAuthorizationTest {

    @org.springframework.boot.SpringBootConfiguration
    @org.springframework.boot.autoconfigure.EnableAutoConfiguration
    static class TestApp {
    }

    /** Carries no @PreAuthorize: reaching it proves the URL rules allowed it. */
    @org.springframework.web.bind.annotation.RestController
    static class ProbeController {

        @org.springframework.web.bind.annotation.DeleteMapping("/api/v1/auth/account")
        String deleteAccount() {
            return "reached";
        }

        @org.springframework.web.bind.annotation.PostMapping("/api/v1/auth/login")
        String login() {
            return "reached";
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @MockBean private JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockBean private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    @MockBean private UserDetailsService userDetailsService;

    @org.springframework.boot.test.context.TestConfiguration
    static class CorsStub {
        @org.springframework.context.annotation.Bean
        CorsConfigurationSource corsConfigurationSource() {
            return request -> new org.springframework.web.cors.CorsConfiguration();
        }
    }

    /**
     * A mocked Filter swallows the chain and a mocked entry point writes no
     * status; both have to be taught to behave, or every assertion here passes
     * as a bare 200 having tested nothing.
     */
    @org.junit.jupiter.api.BeforeEach
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
            ((jakarta.servlet.http.HttpServletResponse) invocation.getArgument(1))
                    .sendError(jakarta.servlet.http.HttpServletResponse.SC_UNAUTHORIZED);
            return null;
        }).when(jwtAuthenticationEntryPoint).commence(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    @WithMockUser(roles = "RESTAURANT_OWNER")
    @DisplayName("a signed-in user can delete their own account")
    void authenticatedUserMayDelete() throws Exception {
        mockMvc.perform(delete("/api/v1/auth/account").with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "CONSUMER")
    @DisplayName("an optional reason body is accepted, not rejected")
    void reasonBodyAccepted() throws Exception {
        mockMvc.perform(delete("/api/v1/auth/account")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"switching platforms\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("an anonymous caller gets 401, not 403")
    void anonymousIsUnauthorized() throws Exception {
        // THE regression this guards: /api/v1/auth/** is permitAll, so without
        // the explicit rule the request would sail through the filter chain and
        // be refused later by method security with the wrong status.
        mockMvc.perform(delete("/api/v1/auth/account").with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("the rest of /auth stays public")
    void loginRemainsPublic() throws Exception {
        // Narrowing one path must not close the door on signing in.
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/api/v1/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());
    }
}
