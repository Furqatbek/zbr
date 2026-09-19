package com.fooddelivery.common.config;

import com.fooddelivery.auth.security.JwtAuthenticationEntryPoint;
import com.fooddelivery.auth.security.JwtAuthenticationFilter;
import com.fooddelivery.integration.partner.security.PartnerAuthenticationFilter;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The URL rules guarding the partner API.
 *
 * <p>Two separate gates protect these endpoints and this covers the first one.
 * The capability checks inside the services are the second, and a controller
 * test cannot see the URL rules at all — as the courier registration deadlock
 * showed, when the two disagree the method rule never runs.
 *
 * <p>What matters most here is the direction nobody thinks to test: a partner
 * key must not be a way into the rest of the platform, and a user session —
 * however privileged — must not be a way into the partner API. A partner key
 * carries authority over many restaurants; an admin session is a person.
 */
@ExtendWith(SpringExtension.class)
@WebMvcTest(controllers = PartnerEndpointAuthorizationTest.ProbeController.class)
@Import({SecurityConfig.class, PartnerEndpointAuthorizationTest.ProbeController.class,
        PartnerEndpointAuthorizationTest.CorsStub.class})
@DisplayName("Partner endpoint authorization")
class PartnerEndpointAuthorizationTest {

    @org.springframework.boot.SpringBootConfiguration
    @org.springframework.boot.autoconfigure.EnableAutoConfiguration
    static class TestApp {
    }

    /** No @PreAuthorize: reaching it proves the URL rules allowed the request. */
    @org.springframework.web.bind.annotation.RestController
    static class ProbeController {

        @org.springframework.web.bind.annotation.PatchMapping(
                "/api/v1/partner/venues/{venue}/menu/items/{item}")
        String updateItem() {
            return "reached";
        }

        @org.springframework.web.bind.annotation.PostMapping("/api/v1/partner/orders/{ref}/status")
        String reportStatus() {
            return "reached";
        }

        @org.springframework.web.bind.annotation.GetMapping("/api/v1/admin/partners/1/keys")
        String adminKeys() {
            return "reached";
        }

        @org.springframework.web.bind.annotation.GetMapping("/api/v1/orders/1")
        String someOtherApi() {
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

    /**
     * Both mocked filters must be taught to continue the chain, and the entry
     * point to write a status — otherwise every assertion here passes as a bare
     * 200 having exercised nothing.
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
    @WithMockUser(roles = "PARTNER")
    @DisplayName("a partner can reach the partner menu API")
    void partnerMayUpdateMenu() throws Exception {
        mockMvc.perform(patch("/api/v1/partner/venues/venue-55/menu/items/4417")
                        .with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "PARTNER")
    @DisplayName("a partner can report order status")
    void partnerMayReportStatus() throws Exception {
        mockMvc.perform(post("/api/v1/partner/orders/FD-20260919-A7K2M9/status")
                        .with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ACCEPTED\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("an anonymous caller gets nowhere near the partner API")
    void anonymousRefused() throws Exception {
        mockMvc.perform(patch("/api/v1/partner/venues/venue-55/menu/items/4417")
                        .with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("even an admin session cannot use the partner API")
    void adminSessionIsNotAPartner() throws Exception {
        // Not pedantry. These endpoints are written assuming the caller is a
        // partner with a venue grant; an admin has no grant, so the capability
        // checks inside would behave in ways nobody designed. Admins administer
        // partners through /admin/partners instead.
        mockMvc.perform(patch("/api/v1/partner/venues/venue-55/menu/items/4417")
                        .with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "RESTAURANT_OWNER")
    @DisplayName("a restaurant owner cannot use the partner API")
    void restaurantOwnerIsNotAPartner() throws Exception {
        mockMvc.perform(patch("/api/v1/partner/venues/venue-55/menu/items/4417")
                        .with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "PARTNER")
    @DisplayName("a partner key is not a way into the rest of the platform")
    void partnerCannotReachOtherApis() throws Exception {
        // The containment that matters. A leaked partner key must buy nothing
        // beyond the venues it was granted — not orders, not users, not the
        // vendor API.
        mockMvc.perform(get("/api/v1/orders/1")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "PARTNER")
    @DisplayName("a partner cannot administer partners")
    void partnerCannotSelfAdminister() throws Exception {
        // Otherwise a partner could grant itself another venue, which is the
        // whole access model undone in one call.
        mockMvc.perform(get("/api/v1/admin/partners/1/keys")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("an admin administers partners")
    void adminMayAdminister() throws Exception {
        mockMvc.perform(get("/api/v1/admin/partners/1/keys")).andExpect(status().isOk());
    }
}
