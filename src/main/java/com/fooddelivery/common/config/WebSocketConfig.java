package com.fooddelivery.common.config;

import com.fooddelivery.auth.security.JwtService;
import com.fooddelivery.auth.security.UserPrincipal;
import com.fooddelivery.auth.service.LastSeenService;
import io.jsonwebtoken.ExpiredJwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket configuration for real-time updates.
 * Supports STOMP over WebSocket for order status updates,
 * courier location streaming, and kitchen ticket pushes.
 */
@Configuration
@EnableWebSocketMessageBroker
@Order(Ordered.HIGHEST_PRECEDENCE + 99)
@RequiredArgsConstructor
@Slf4j
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    // Lazy provider avoids an init cycle (authorizer -> OrderService -> messaging infra).
    private final ObjectProvider<WebSocketDestinationAuthorizer> subscriptionAuthorizer;
    private final LastSeenService lastSeenService;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Enable a simple memory-based message broker
        // For production, consider using RabbitMQ STOMP plugin
        config.enableSimpleBroker(
                "/topic",  // For broadcasting to all subscribers
                "/queue"   // For point-to-point messaging
        );

        // Set prefix for messages from client to server
        config.setApplicationDestinationPrefixes("/app");

        // Set prefix for user-specific destinations
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Native WebSocket endpoint (for mobile apps and native WebSocket clients)
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*");

        // SockJS endpoint (for web browsers with fallback support)
        registry.addEndpoint("/ws-sockjs")
                .setAllowedOriginPatterns("*")
                .withSockJS()
                .setHeartbeatTime(25000);

        // Alias for backwards compatibility
        registry.addEndpoint("/ws-native")
                .setAllowedOriginPatterns("*");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(
                        message, StompHeaderAccessor.class);

                if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
                    String authHeader = accessor.getFirstNativeHeader("Authorization");

                    if (authHeader != null && authHeader.startsWith("Bearer ")) {
                        String token = authHeader.substring(7);

                        try {
                            String username = jwtService.extractUsername(token);

                            if (username != null) {
                                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                                if (jwtService.isTokenValid(token, userDetails)) {
                                    UsernamePasswordAuthenticationToken authentication =
                                            new UsernamePasswordAuthenticationToken(
                                                    userDetails,
                                                    null,
                                                    userDetails.getAuthorities()
                                            );

                                    SecurityContextHolder.getContext().setAuthentication(authentication);
                                    accessor.setUser(authentication);

                                    log.debug("WebSocket connection authenticated for user: {}", username);

                                    // A socket connect is the strongest signal
                                    // the app is open, and it does not pass
                                    // through JwtAuthenticationFilter (/ws is
                                    // skipped there), so record it here too.
                                    if (userDetails instanceof UserPrincipal p) {
                                        lastSeenService.touch(p.getId());
                                    }
                                } else {
                                    log.warn("WebSocket authentication failed: Token validation failed for user {}. " +
                                            "Please refresh your access token using /api/v1/auth/refresh endpoint.", username);
                                }
                            }
                        } catch (ExpiredJwtException e) {
                            log.warn("WebSocket authentication failed: Access token expired. " +
                                    "Please refresh your access token using /api/v1/auth/refresh endpoint and reconnect. " +
                                    "Token expired at: {}", e.getClaims().getExpiration());
                        } catch (Exception e) {
                            log.warn("WebSocket authentication failed: {}", e.getMessage());
                        }
                    }

                    // Reject the CONNECT if no valid principal was established: a
                    // missing/invalid/expired token must not open an (unauthenticated)
                    // socket. Subscribe authz already fails closed, but this stops the
                    // session from being created at all.
                    if (accessor.getUser() == null) {
                        throw new AccessDeniedException(
                                "WebSocket CONNECT requires a valid 'Authorization: Bearer <token>' header");
                    }
                }

                // Authorize SUBSCRIBE frames per destination so a connected client cannot
                // read another user's data on broadcast /topic/** destinations.
                if (accessor != null && StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
                    String destination = accessor.getDestination();
                    Authentication auth = accessor.getUser() instanceof Authentication a ? a : null;
                    if (!subscriptionAuthorizer.getObject().canSubscribe(auth, destination)) {
                        log.warn("Rejected WebSocket SUBSCRIBE to '{}' for {}", destination,
                                auth != null ? auth.getName() : "unauthenticated session");
                        throw new AccessDeniedException("Not authorized to subscribe to " + destination);
                    }
                }

                return message;
            }
        });
    }
}
