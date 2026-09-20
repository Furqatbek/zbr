package com.fooddelivery.restaurant.service;

import com.fooddelivery.common.dto.PagedResponse;
import com.fooddelivery.delivery.eta.DeliveryEta;
import com.fooddelivery.delivery.eta.DeliveryEtaService;
import com.fooddelivery.restaurant.dto.RestaurantDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Stamps "2.4 km away, 35-45 min" onto restaurants for one customer.
 *
 * <p>Deliberately a separate component rather than a few lines inside
 * {@link RestaurantService}: {@code getRestaurantById} is {@code @Cacheable},
 * and a distance computed for one customer's phone written inside that method
 * would be cached under the restaurant's id and handed to the next customer,
 * who is somewhere else entirely. Enriching after the cached read is what keeps
 * a per-customer figure out of a shared cache.
 *
 * <p>For the same reason every stamp goes onto a copy. The object that comes
 * back from a cache read may be the cached instance itself, depending on how
 * the cache is configured; writing into it would be the same bug by another
 * route.
 */
@Component
@RequiredArgsConstructor
public class RestaurantEtaEnricher {

    private final DeliveryEtaService etaService;

    /**
     * @param customerLat null when the request carried no location, in which
     *                    case the restaurant is returned untouched and the app
     *                    shows what it showed before: the preparation time
     */
    public RestaurantDto withEta(RestaurantDto restaurant, BigDecimal customerLat, BigDecimal customerLng) {
        if (restaurant == null || customerLat == null || customerLng == null) {
            return restaurant;
        }
        Optional<DeliveryEta> eta = etaService.browsing(
                restaurant.getLatitude(), restaurant.getLongitude(),
                restaurant.getAveragePrepTimeMinutes(), customerLat, customerLng);

        return eta.map(e -> restaurant.toBuilder()
                        .distanceKm(e.getDistanceKm())
                        .etaMinutesMin(e.getEtaMinutesMin())
                        .etaMinutesMax(e.getEtaMinutesMax())
                        .build())
                .orElse(restaurant);
    }

    public List<RestaurantDto> withEta(List<RestaurantDto> restaurants,
                                       BigDecimal customerLat, BigDecimal customerLng) {
        if (restaurants == null || customerLat == null || customerLng == null) {
            return restaurants;
        }
        return restaurants.stream().map(r -> withEta(r, customerLat, customerLng)).toList();
    }

    public PagedResponse<RestaurantDto> withEta(PagedResponse<RestaurantDto> page,
                                                BigDecimal customerLat, BigDecimal customerLng) {
        if (page == null || customerLat == null || customerLng == null) {
            return page;
        }
        page.setContent(withEta(page.getContent(), customerLat, customerLng));
        return page;
    }
}
