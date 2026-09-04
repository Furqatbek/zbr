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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The URL rules in SecurityConfig and the @PreAuthorize rules on the controller
 * are two independent gates, and the URL rules run FIRST — inside the filter
 * chain, long before the method is invoked. When they disagree, the method rule
 * is unreachable and the endpoint is dead in a way no controller test can see.
 *
 * <p>That is what happened to courier registration:
 * {@code /api/v1/couriers/**} required ROLE_COURIER, but
 * {@code POST /couriers/register} is the endpoint that GRANTS ROLE_COURIER. You
 * needed the role to obtain the role. The controller said
 * {@code hasRole('CONSUMER')} and never ran. The platform ended up with courier
 * users and zero courier profiles, and it looked like a mobile-app bug.
 *
 * <p>These tests exercise the real filter chain, so they fail if the URL rules
 * and the method rules drift apart again.
 */
@ExtendWith(SpringExtension.class)
@WebMvcTest(controllers = CourierEndpointAuthorizationTest.ProbeController.class)
@Import({SecurityConfig.class, CourierEndpointAuthorizationTest.ProbeController.class,
        CourierEndpointAuthorizationTest.CorsStub.class})
@DisplayName("Courier endpoint authorization")
class CourierEndpointAuthorizationTest {

    /**
     * A bare @SpringBootConfiguration so the slice does NOT pick up the real
     * application class, which carries @EnableJpaAuditing and would demand a
     * populated JPA metamodel this web-layer test has no use for. Spring
     * searches upward from the test class, so this nested one wins.
     */
    @org.springframework.boot.SpringBootConfiguration
    @org.springframework.boot.autoconfigure.EnableAutoConfiguration
    static class TestApp {
    }

    /**
     * Stands in for CourierController. It carries no @PreAuthorize, so anything
     * that reaches it proves the URL rules let the request through — which is
     * exactly and only what is under test here.
     */
    @org.springframework.web.bind.annotation.RestController
    static class ProbeController {

        @org.springframework.web.bind.annotation.PostMapping("/api/v1/couriers/register")
        String register() {
            return "reached";
        }

        @org.springframework.web.bind.annotation.GetMapping("/api/v1/couriers/me")
        String me() {
            return "reached";
        }

        @org.springframework.web.bind.annotation.GetMapping("/api/v1/couriers/available")
        String available() {
            return "reached";
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @MockBean private JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockBean private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    @MockBean private UserDetailsService userDetailsService;

    /**
     * A real bean, NOT a @MockBean. Spring's own HandlerMappingIntrospector
     * implements CorsConfigurationSource, so mocking that interface by type
     * replaces it and the context fails to start — the bean must be supplied by
     * the name SecurityConfig injects, exactly as production does.
     */
    @org.springframework.boot.test.context.TestConfiguration
    static class CorsStub {
        @org.springframework.context.annotation.Bean
        CorsConfigurationSource corsConfigurationSource() {
            return request -> new org.springframework.web.cors.CorsConfiguration();
        }
    }

    /**
     * A Mockito mock of a Filter does nothing when invoked — including not
     * calling the rest of the chain — so every request would end as a bare 200
     * having reached neither the authorization rules nor the controller. Three
     * of these tests "passed" that way before this was added, testing nothing.
     * Authentication itself comes from @WithMockUser, so the filter only has to
     * step aside.
     */
    @org.junit.jupiter.api.BeforeEach
    void jwtFilterPassesThrough() throws Exception {
        org.mockito.Mockito.doAnswer(invocation -> {
            jakarta.servlet.FilterChain chain = invocation.getArgument(2);
            chain.doFilter(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(jwtAuthenticationFilter).doFilter(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());

        // Same trap one layer down: a mocked entry point writes no status, so a
        // rejected anonymous request would still surface as 200 and the "must
        // be authenticated" assertion would pass for the wrong reason.
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
    @WithMockUser(roles = "CONSUMER")
    @DisplayName("a plain consumer can reach courier registration")
    void consumerMayRegister() throws Exception {
        // THE regression. Before the fix this was 403: the blanket rule wanted
        // ROLE_COURIER, which only registering can grant.
        mockMvc.perform(post("/api/v1/couriers/register")
                        .with(org.springframework.security.test.web.servlet.request
                                .SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"vehicleType\":\"MOTORCYCLE\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("registration still requires authentication")
    void anonymousMayNotRegister() throws Exception {
        // Opening the path must not open it to everyone — the caller still has
        // to be a signed-in user, since the courier profile attaches to them.
        mockMvc.perform(post("/api/v1/couriers/register")
                        .with(org.springframework.security.test.web.servlet.request
                                .SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"vehicleType\":\"MOTORCYCLE\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "CONSUMER")
    @DisplayName("a consumer still cannot reach the rest of the courier API")
    void consumerMayNotUseCourierEndpoints() throws Exception {
        // The exemption must be for /register alone.
        mockMvc.perform(get("/api/v1/couriers/me")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "COURIER")
    @DisplayName("a courier can reach the courier API")
    void courierMayUseCourierEndpoints() throws Exception {
        mockMvc.perform(get("/api/v1/couriers/me")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "RESTAURANT_OWNER")
    @DisplayName("a restaurant owner can list available couriers")
    void restaurantOwnerMayListAvailableCouriers() throws Exception {
        // Same contradiction, second instance: the controller allows
        // RESTAURANT_OWNER here, but the URL rule rejected them first.
        mockMvc.perform(get("/api/v1/couriers/available")).andExpect(status().isOk());
    }
}
