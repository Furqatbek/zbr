package com.fooddelivery.common.config;

import com.fooddelivery.auth.entity.Role;
import com.fooddelivery.auth.security.UserPrincipal;
import com.fooddelivery.courier.repository.CourierRepository;
import com.fooddelivery.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Authorizes STOMP SUBSCRIBE frames per destination.
 *
 * The broker publishes some payloads that contain personal data to broadcast
 * {@code /topic/**} destinations keyed by an id (order id, user id, courier id).
 * Without this check any connected client could subscribe to another user's
 * destination and harvest customer names/phones/addresses, read other users'
 * notifications, or track couriers. Non-sensitive destinations are allowed;
 * sensitive per-id destinations are checked against the authenticated principal
 * and fail closed when the session is unauthenticated.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketDestinationAuthorizer {

    private final OrderService orderService;
    private final CourierRepository courierRepository;

    private static final Pattern USER_NOTIFICATIONS = Pattern.compile("^/topic/users/(\\d+)/notifications$");
    private static final Pattern ORDER = Pattern.compile("^/topic/orders/(\\d+)$");
    private static final Pattern ORDER_TAKEN = Pattern.compile("^/topic/orders/(\\d+)/taken$");
    private static final Pattern COURIER_LOCATION = Pattern.compile("^/topic/couriers/(\\d+)/location$");

    /**
     * @return true if the authenticated user may subscribe to this destination.
     */
    public boolean canSubscribe(Authentication authentication, String destination) {
        if (destination == null) {
            return true;
        }
        UserPrincipal principal = principalOf(authentication);

        Matcher m = USER_NOTIFICATIONS.matcher(destination);
        if (m.matches()) {
            if (principal == null) return false;
            return principal.getId().equals(Long.parseLong(m.group(1))) || isAdminOrPlatform(principal);
        }

        m = ORDER.matcher(destination);
        if (m.matches()) {
            // Full OrderDto with customer name/phone/address — party-to-order only.
            if (principal == null) return false;
            return isPartyToOrder(principal, Long.parseLong(m.group(1)));
        }

        if (ORDER_TAKEN.matcher(destination).matches()) {
            // "order taken" is an operational signal broadcast to couriers so they can
            // dismiss stale offers; it carries no customer PII. Any authenticated user.
            return principal != null;
        }

        m = COURIER_LOCATION.matcher(destination);
        if (m.matches()) {
            if (principal == null) return false;
            if (isAdminOrPlatform(principal)) return true;
            long courierId = Long.parseLong(m.group(1));
            // Only the courier themselves may stream their own live location.
            // Consumers get the assigned courier's position via GET /orders/{id}/tracking.
            return courierRepository.findByUserId(principal.getId())
                    .map(c -> c.getId().equals(courierId))
                    .orElse(false);
        }

        // Everything else (the broadcast offer feed, /queue, /user/**, /app) is allowed.
        return true;
    }

    private boolean isPartyToOrder(UserPrincipal principal, long orderId) {
        try {
            orderService.validateOrderAccess(
                    orderId, principal.getId(),
                    isAdminOrPlatform(principal), isRestaurant(principal), isCourier(principal));
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    private UserPrincipal principalOf(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal p) {
            return p;
        }
        return null;
    }

    private boolean hasAny(UserPrincipal p, Role... roles) {
        Set<Role> owned = p.getRoles();
        for (Role role : roles) {
            if (owned.contains(role)) return true;
        }
        return false;
    }

    private boolean isAdminOrPlatform(UserPrincipal p) {
        return hasAny(p, Role.ADMIN, Role.PLATFORM);
    }

    private boolean isRestaurant(UserPrincipal p) {
        return hasAny(p, Role.RESTAURANT_OWNER, Role.RESTAURANT_STAFF, Role.RESTAURANT_MANAGER);
    }

    private boolean isCourier(UserPrincipal p) {
        return hasAny(p, Role.COURIER);
    }
}
