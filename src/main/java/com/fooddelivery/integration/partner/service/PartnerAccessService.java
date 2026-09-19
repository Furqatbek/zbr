package com.fooddelivery.integration.partner.service;

import com.fooddelivery.common.exception.BusinessException;
import com.fooddelivery.common.exception.ResourceNotFoundException;
import com.fooddelivery.integration.partner.entity.Partner;
import com.fooddelivery.integration.partner.entity.PartnerCapability;
import com.fooddelivery.integration.partner.entity.PartnerVenueGrant;
import com.fooddelivery.integration.partner.repository.PartnerRepository;
import com.fooddelivery.integration.partner.repository.PartnerVenueGrantRepository;
import com.fooddelivery.integration.partner.security.PartnerPrincipal;
import com.fooddelivery.restaurant.entity.Restaurant;
import com.fooddelivery.restaurant.service.RestaurantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

/**
 * The single gate every partner request passes through.
 *
 * <p>One method resolves the venue and checks the capability together, because
 * separating them is how a controller ends up doing one and forgetting the
 * other. There is no way to obtain a restaurant here without having said what
 * you intend to do to it.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PartnerAccessService {

    private final PartnerRepository partnerRepository;
    private final PartnerVenueGrantRepository grantRepository;
    private final RestaurantService restaurantService;

    /**
     * Whether an order's paymentMode reflects how it will actually be paid.
     *
     * <p>False until the apps set it. While it is false, a venue cannot be
     * switched on for live order push — see grantVenue.
     */
    @org.springframework.beans.factory.annotation.Value(
            "${app.integration.partner.payment-mode-authoritative:false}")
    private boolean paymentModeAuthoritative;

    /**
     * Resolve the partner's own venue id to one of our restaurants, having
     * confirmed they may do this to it.
     *
     * @throws ResourceNotFoundException when no grant maps that venue id —
     *         deliberately the same answer as a venue that exists but belongs
     *         to another partner, so the endpoint cannot be used to discover
     *         which venue ids are live.
     */
    @Transactional(readOnly = true)
    public Restaurant resolveVenue(PartnerPrincipal principal, String externalVenueId,
                                   PartnerCapability required) {
        PartnerVenueGrant grant = grantRepository
                .findByPartnerIdAndExternalVenueId(principal.getPartnerId(), externalVenueId)
                .orElseThrow(() -> {
                    log.warn("Partner {} has no grant for venue '{}'",
                            principal.getPartnerCode(), externalVenueId);
                    return new ResourceNotFoundException("No venue is mapped to id: " + externalVenueId);
                });

        if (!grant.allows(required)) {
            // Distinct from "no grant": the mapping exists and they are entitled
            // to know the capability was the problem, since that is something
            // they can ask us to change.
            log.warn("Partner {} lacks {} on venue '{}'",
                    principal.getPartnerCode(), required, externalVenueId);
            throw new AccessDeniedException(
                    "This key is not permitted to " + describe(required) + " for venue " + externalVenueId);
        }

        return grant.getRestaurant();
    }

    /**
     * Resolve the venue that an order belongs to, confirming the partner holds
     * the capability over that restaurant.
     *
     * <p>Orders are addressed by OUR reference rather than the partner's, so
     * this is what stops one partner reporting status on another's orders — or
     * on an order from a restaurant they were never granted.
     */
    @Transactional(readOnly = true)
    public void requireCapabilityOnRestaurant(PartnerPrincipal principal, Long restaurantId,
                                              PartnerCapability required) {
        PartnerVenueGrant grant = grantRepository
                .findByPartnerIdAndRestaurantId(principal.getPartnerId(), restaurantId)
                .orElseThrow(() -> {
                    log.warn("Partner {} has no grant for restaurant {}",
                            principal.getPartnerCode(), restaurantId);
                    return new ResourceNotFoundException("Order not found");
                });

        if (!grant.allows(required)) {
            throw new AccessDeniedException("This key is not permitted to " + describe(required)
                    + " for this venue");
        }
    }

    private String describe(PartnerCapability capability) {
        return switch (capability) {
            case MENU_WRITE -> "change the menu";
            case ORDER_STATUS_WRITE -> "report order status";
        };
    }

    // --- administration ----------------------------------------------------

    @Transactional
    public PartnerVenueGrant grantVenue(Long partnerId, Long restaurantId, String externalVenueId,
                                        Set<PartnerCapability> capabilities, Boolean pushOrders) {
        Partner partner = partnerRepository.findById(partnerId)
                .orElseThrow(() -> new ResourceNotFoundException("Partner", "id", partnerId));
        Restaurant restaurant = restaurantService.getRestaurantEntityById(restaurantId);

        if (externalVenueId == null || externalVenueId.isBlank()) {
            throw new BusinessException("The partner's venue id is required");
        }

        // Update in place when the mapping already exists, so re-granting is not
        // a unique-constraint error someone has to work around by deleting.
        PartnerVenueGrant grant = grantRepository
                .findByPartnerIdAndRestaurantId(partnerId, restaurantId)
                .orElseGet(() -> PartnerVenueGrant.builder()
                        .partner(partner)
                        .restaurant(restaurant)
                        .build());

        grant.setExternalVenueId(externalVenueId.trim());
        grant.setCapabilities(capabilities == null ? Set.of() : Set.copyOf(capabilities));
        if (pushOrders != null) {
            if (pushOrders && !paymentModeAuthoritative) {
                // Every order currently says PREPAID because the apps do not
                // set a payment mode yet. To a partner's till that is not
                // decoration — it prints as a paid order, and a counter hand
                // gives a bag to a courier who owes nothing.
                //
                // Restos asked us not to flip this until the field is real, and
                // said they cannot detect the change from their side. A promise
                // someone has to remember is worse than a switch, so it is a
                // switch: set app.integration.partner.payment-mode-authoritative
                // once the apps send it.
                throw new BusinessException("Order push cannot be enabled while paymentMode is a "
                        + "constant. Every order would be announced as PREPAID, and a cash order "
                        + "announced as prepaid is food the venue hands over and collects nothing "
                        + "for. Set app.integration.partner.payment-mode-authoritative once the "
                        + "apps send it for real.");
            }
            grant.setPushOrders(pushOrders);
        }

        PartnerVenueGrant saved = grantRepository.save(grant);
        log.info("Partner {} granted {} on restaurant {} (their venue id '{}'), order push {}",
                partner.getCode(), saved.getCapabilities(), restaurantId, saved.getExternalVenueId(),
                saved.isPushOrders() ? "ON" : "off");
        return saved;
    }

    /**
     * Configure where we send this partner's orders, and with what credential.
     *
     * <p>The credential is one they issued us. It is stored, never returned,
     * and never logged — a partner key in a log line is a partner key in a log
     * aggregator, a backup and a screenshot.
     */
    @Transactional
    public Partner configureOutbound(Long partnerId, String baseUrl, String apiKey, String authHeader) {
        Partner partner = partnerRepository.findById(partnerId)
                .orElseThrow(() -> new ResourceNotFoundException("Partner", "id", partnerId));

        if (baseUrl != null) {
            partner.setOutboundBaseUrl(baseUrl.isBlank() ? null : baseUrl.trim());
        }
        // Null leaves the existing credential alone, so the URL can be changed
        // without re-sending a secret. Blank clears it deliberately.
        if (apiKey != null) {
            partner.setOutboundApiKey(apiKey.isBlank() ? null : apiKey.trim());
        }
        if (authHeader != null) {
            partner.setOutboundAuthHeader(authHeader.isBlank() ? null : authHeader.trim());
        }

        Partner saved = partnerRepository.save(partner);
        log.info("Partner {} outbound configured: url={}, credential {}",
                saved.getCode(), saved.getOutboundBaseUrl(),
                saved.getOutboundApiKey() != null ? "set" : "absent");
        return saved;
    }

    @Transactional
    public void revokeVenue(Long partnerId, Long restaurantId) {
        grantRepository.findByPartnerIdAndRestaurantId(partnerId, restaurantId)
                .ifPresent(grant -> {
                    grantRepository.delete(grant);
                    log.warn("Partner {} lost all access to restaurant {}", partnerId, restaurantId);
                });
    }

    @Transactional(readOnly = true)
    public List<PartnerVenueGrant> listGrants(Long partnerId) {
        return grantRepository.findByPartnerId(partnerId);
    }
}
