package com.fooddelivery.common.config;

import com.fooddelivery.auth.security.UserPrincipal;
import com.fooddelivery.courier.entity.Courier;
import com.fooddelivery.courier.entity.CourierStatus;
import com.fooddelivery.courier.repository.CourierRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Listens for WebSocket connect/disconnect events.
 * Automatically sets courier status to OFFLINE when they disconnect.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketEventListener {

    private final CourierRepository courierRepository;

    @EventListener
    public void handleSessionConnect(SessionConnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();
        Principal user = accessor.getUser();

        if (user != null) {
            log.info("WebSocket connected: sessionId={}, user={}", sessionId, user.getName());
        }
    }

    @EventListener
    @Transactional
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();
        Principal user = event.getUser();

        if (user == null) {
            log.debug("WebSocket disconnected: sessionId={}, no authenticated user", sessionId);
            return;
        }

        log.info("WebSocket disconnected: sessionId={}, user={}", sessionId, user.getName());

        // Check if the disconnected user is a courier and set them offline
        try {
            UserPrincipal principal = extractUserPrincipal(user);
            if (principal == null) {
                return;
            }

            boolean isCourier = principal.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_COURIER"));

            if (!isCourier) {
                return;
            }

            Optional<Courier> courierOpt = courierRepository.findByUserId(principal.getId());
            if (courierOpt.isEmpty()) {
                return;
            }

            Courier courier = courierOpt.get();
            CourierStatus currentStatus = courier.getStatus();

            // Only set to OFFLINE if courier was actively online (AVAILABLE or ON_BREAK)
            // Don't change BUSY (they have active deliveries), SUSPENDED, or PENDING_APPROVAL
            if (currentStatus == CourierStatus.AVAILABLE || currentStatus == CourierStatus.ON_BREAK) {
                courier.setStatus(CourierStatus.OFFLINE);
                courier.setLastActiveAt(LocalDateTime.now());
                courierRepository.save(courier);
                log.info("Courier {} (userId={}) auto-set to OFFLINE on WebSocket disconnect",
                        courier.getId(), principal.getId());
            }
        } catch (Exception e) {
            log.warn("Failed to update courier status on disconnect: {}", e.getMessage());
        }
    }

    private UserPrincipal extractUserPrincipal(Principal principal) {
        if (principal instanceof UsernamePasswordAuthenticationToken auth) {
            Object p = auth.getPrincipal();
            if (p instanceof UserPrincipal userPrincipal) {
                return userPrincipal;
            }
        }
        return null;
    }
}
