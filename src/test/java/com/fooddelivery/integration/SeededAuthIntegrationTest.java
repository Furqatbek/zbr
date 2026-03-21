package com.fooddelivery.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fooddelivery.auth.dto.LoginRequest;
import com.fooddelivery.auth.entity.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for authentication using seeded migration data.
 * Tests login for all roles: ADMIN, PLATFORM, RESTAURANT_OWNER, CONSUMER, COURIER.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
@DisplayName("Seeded Auth Integration Tests")
class SeededAuthIntegrationTest {

    private static final String SEEDED_PASSWORD = "password";

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("fooddelivery_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        // Enable Flyway to run migrations including seed data
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @ParameterizedTest(name = "Login as {0} with email {1}")
    @CsvSource({
            "ADMIN, admin@fooddelivery.com",
            "PLATFORM, platform@fooddelivery.com",
            "RESTAURANT_OWNER, owner@pizzapalace.com",
            "CONSUMER, john.doe@example.com",
            "COURIER, courier@fooddelivery.com"
    })
    @DisplayName("Should login successfully with seeded users")
    void shouldLoginWithSeededUser(String expectedRole, String email) throws Exception {
        LoginRequest loginRequest = LoginRequest.builder()
                .emailOrPhone(email)
                .password(SEEDED_PASSWORD)
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").exists())
                .andExpect(jsonPath("$.data.refreshToken").exists())
                .andExpect(jsonPath("$.data.email").value(email))
                .andExpect(jsonPath("$.data.roles", hasItem(expectedRole)));
    }

    @Test
    @DisplayName("Should login as restaurant owner and access restaurant endpoints")
    void shouldLoginAsRestaurantOwnerAndAccessRestaurant() throws Exception {
        // Login as restaurant owner
        LoginRequest loginRequest = LoginRequest.builder()
                .emailOrPhone("owner@pizzapalace.com")
                .password(SEEDED_PASSWORD)
                .build();

        String response = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.roles", hasItem("RESTAURANT_OWNER")))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String accessToken = objectMapper.readTree(response).path("data").path("accessToken").asText();

        // Access /auth/me endpoint with token
        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("owner@pizzapalace.com"));
    }

    @Test
    @DisplayName("Should fail login with wrong password")
    void shouldFailLoginWithWrongPassword() throws Exception {
        LoginRequest loginRequest = LoginRequest.builder()
                .emailOrPhone("owner@pizzapalace.com")
                .password("wrongpassword")
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Should fail login with non-existent email")
    void shouldFailLoginWithNonExistentEmail() throws Exception {
        LoginRequest loginRequest = LoginRequest.builder()
                .emailOrPhone("nonexistent@example.com")
                .password(SEEDED_PASSWORD)
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Should login as admin and have multiple roles")
    void shouldLoginAsAdminWithMultipleRoles() throws Exception {
        LoginRequest loginRequest = LoginRequest.builder()
                .emailOrPhone("admin@fooddelivery.com")
                .password(SEEDED_PASSWORD)
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.roles", hasItem("ADMIN")))
                .andExpect(jsonPath("$.data.roles", hasItem("PLATFORM")))
                .andExpect(jsonPath("$.data.roles", hasItem("RESTAURANT_OWNER")))
                .andExpect(jsonPath("$.data.roles", hasItem("COURIER")))
                .andExpect(jsonPath("$.data.roles", hasItem("CONSUMER")));
    }

    @Test
    @DisplayName("Should login using phone number")
    void shouldLoginUsingPhoneNumber() throws Exception {
        // Consumer's phone from seed data
        LoginRequest loginRequest = LoginRequest.builder()
                .emailOrPhone("+1234567893")
                .password(SEEDED_PASSWORD)
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("john.doe@example.com"))
                .andExpect(jsonPath("$.data.roles", hasItem("CONSUMER")));
    }
}
