package com.fooddelivery.restaurant.service;

import com.fooddelivery.common.dto.PagedResponse;
import com.fooddelivery.delivery.eta.DeliveryEta;
import com.fooddelivery.delivery.eta.DeliveryEtaService;
import com.fooddelivery.courier.entity.VehicleType;
import com.fooddelivery.restaurant.dto.RestaurantDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Stamping a per-customer distance onto a shared restaurant.
 *
 * <p>The hazard this component exists for is one test below: {@code
 * getRestaurantById} is {@code @Cacheable}, so anything written into the object
 * it returns is written into an entry keyed by restaurant id and handed to the
 * next customer, who is somewhere else.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Restaurant distance and ETA")
class RestaurantEtaEnricherTest {

    private static final BigDecimal LAT = new BigDecimal("41.326000");
    private static final BigDecimal LNG = new BigDecimal("69.280000");

    @Mock private DeliveryEtaService etaService;

    private RestaurantEtaEnricher enricher() {
        return new RestaurantEtaEnricher(etaService);
    }

    private RestaurantDto restaurant(long id) {
        return RestaurantDto.builder()
                .id(id)
                .name("Plov Centre")
                .latitude(new BigDecimal("41.311081"))
                .longitude(new BigDecimal("69.240562"))
                .averagePrepTimeMinutes(25)
                .build();
    }

    private void etaIs(double km, int min, int max) {
        when(etaService.browsing(any(), any(), any(), any(), any()))
                .thenReturn(Optional.of(DeliveryEta.builder()
                        .distanceKm(km).prepMinutes(25).travelMinutes(15)
                        .etaMinutesMin(min).etaMinutesMax(max)
                        .vehicle(VehicleType.BICYCLE).vehicleAssumed(true).routed(false)
                        .build()));
    }

    @Test
    @DisplayName("a restaurant is returned with the distance and range stamped on")
    void stampsDistanceAndRange() {
        etaIs(2.4, 35, 45);

        RestaurantDto enriched = enricher().withEta(restaurant(1L), LAT, LNG);

        assertThat(enriched.getDistanceKm()).isEqualTo(2.4);
        assertThat(enriched.getEtaMinutesMin()).isEqualTo(35);
        assertThat(enriched.getEtaMinutesMax()).isEqualTo(45);
    }

    @Test
    @DisplayName("the restaurant it was given is left untouched")
    void doesNotWriteIntoTheCachedObject() {
        // The object handed in may be the one sitting in the cache. Writing
        // this customer's distance into it would serve it to the next.
        etaIs(2.4, 35, 45);
        RestaurantDto cached = restaurant(1L);

        RestaurantDto enriched = enricher().withEta(cached, LAT, LNG);

        assertThat(cached.getDistanceKm()).isNull();
        assertThat(cached.getEtaMinutesMin()).isNull();
        assertThat(enriched).isNotSameAs(cached);
        // And the copy is otherwise the same restaurant.
        assertThat(enriched.getId()).isEqualTo(cached.getId());
        assertThat(enriched.getName()).isEqualTo(cached.getName());
        assertThat(enriched.getAveragePrepTimeMinutes()).isEqualTo(cached.getAveragePrepTimeMinutes());
    }

    @Test
    @DisplayName("no customer location means the response is exactly what it was before")
    void withoutCoordinatesNothingChanges() {
        RestaurantDto original = restaurant(1L);

        RestaurantDto result = enricher().withEta(original, null, null);

        // The app that has not been updated yet, and the customer who refused
        // the location permission, both still get their preparation time.
        assertThat(result).isSameAs(original);
        assertThat(result.getDistanceKm()).isNull();
    }

    @Test
    @DisplayName("a restaurant we cannot place keeps its card without a distance")
    void unplaceableRestaurantIsReturnedUnstamped() {
        when(etaService.browsing(any(), any(), any(), any(), any())).thenReturn(Optional.empty());
        RestaurantDto noCoordinates = RestaurantDto.builder().id(2L).name("Pop-up").build();

        RestaurantDto result = enricher().withEta(noCoordinates, LAT, LNG);

        // Not dropped from the list, and not shown as zero kilometres away.
        assertThat(result.getId()).isEqualTo(2L);
        assertThat(result.getDistanceKm()).isNull();
    }

    @Test
    @DisplayName("every restaurant in a list is stamped")
    void stampsEveryItemInAList() {
        etaIs(2.4, 35, 45);

        List<RestaurantDto> enriched = enricher()
                .withEta(List.of(restaurant(1L), restaurant(2L), restaurant(3L)), LAT, LNG);

        assertThat(enriched).hasSize(3)
                .allSatisfy(r -> assertThat(r.getDistanceKm()).isEqualTo(2.4));
    }

    @Test
    @DisplayName("a page keeps its paging and gains the distances")
    void stampsAPageWithoutLosingPaging() {
        etaIs(2.4, 35, 45);
        PagedResponse<RestaurantDto> page = PagedResponse.<RestaurantDto>builder()
                .content(List.of(restaurant(1L), restaurant(2L)))
                .page(2).size(20).totalElements(41).totalPages(3).build();

        PagedResponse<RestaurantDto> enriched = enricher().withEta(page, LAT, LNG);

        assertThat(enriched.getContent()).allSatisfy(r ->
                assertThat(r.getEtaMinutesMax()).isEqualTo(45));
        assertThat(enriched.getPage()).isEqualTo(2);
        assertThat(enriched.getTotalElements()).isEqualTo(41);
        assertThat(enriched.getTotalPages()).isEqualTo(3);
    }
}
