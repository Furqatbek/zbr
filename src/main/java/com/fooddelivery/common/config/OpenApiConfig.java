package com.fooddelivery.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI/Swagger configuration for API documentation.
 */
@Configuration
public class OpenApiConfig {

    @Value("${server.port:8080}")
    private String serverPort;

    @Bean
    public OpenAPI customOpenAPI() {
        final String securitySchemeName = "bearerAuth";

        return new OpenAPI()
                .info(new Info()
                        .title("Food Delivery Platform API")
                        .description("""
                                Production-ready backend for a food-delivery aggregator service.

                                ## Features
                                - Restaurant onboarding and menu management
                                - Order processing with state machine
                                - Courier assignment and real-time tracking
                                - Payment integration hooks (Stripe-like)
                                - Kitchen Display System (KDS) events
                                - Real-time WebSocket updates
                                - Analytics and referral programs

                                ## Authentication
                                This API uses JWT (JSON Web Token) for authentication.
                                Include the token in the Authorization header:
                                ```
                                Authorization: Bearer <your-access-token>
                                ```

                                ## Rate Limiting
                                API requests are rate-limited to prevent abuse.
                                Default: 60 requests per minute per user/IP.

                                ## WebSocket Endpoints
                                Real-time updates available at:
                                - `/ws` (with SockJS fallback)
                                - `/ws-native` (native WebSocket)

                                Topics:
                                - `/topic/orders/{orderId}` - Order status updates
                                - `/topic/restaurants/{restaurantId}/orders` - Restaurant order updates
                                - `/topic/couriers/{courierId}/location` - Courier location updates
                                - `/topic/users/{userId}/notifications` - User notifications (canonical; subscribe to exactly one notification destination to avoid double-counting)
                                - `/topic/broadcast/notifications` - Platform-wide announcements
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Food Delivery Platform Team")
                                .email("api-support@fooddelivery.com")
                                .url("https://fooddelivery.com"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:" + serverPort)
                                .description("Local Development Server"),
                        new Server()
                                .url("https://api.fooddelivery.com")
                                .description("Production Server")
                ))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .name(securitySchemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Enter your JWT token in the format: Bearer {token}")
                        ));
    }
}
