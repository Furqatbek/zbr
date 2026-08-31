package com.fooddelivery.restaurant.service;

import com.fooddelivery.auth.service.UserService;
import com.fooddelivery.common.service.ImageStorageService;
import com.fooddelivery.restaurant.dto.RestaurantDto;
import com.fooddelivery.restaurant.entity.Restaurant;
import com.fooddelivery.restaurant.entity.RestaurantStatus;
import com.fooddelivery.restaurant.mapper.RestaurantMapper;
import com.fooddelivery.restaurant.repository.RestaurantRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * A restaurant is cached under TWO keys — its id and 'slug:&lt;slug&gt;' — and
 * every mutating method used to evict only the id. The stale slug entry then
 * served the pre-update row for the rest of the 5-minute TTL, so a restaurant
 * the owner had just closed still read as open on /restaurants/slug/{slug}.
 *
 * <p>Plain Mockito cannot catch that: caching is applied by a proxy, so a
 * directly-constructed service has no cache at all and every assertion passes
 * whether the annotations are right or wrong. This test runs the service
 * through a real Spring cache proxy so the annotations themselves are what is
 * under test.
 */
@DisplayName("Restaurant cache eviction")
class RestaurantCacheEvictionTest {

    private static final Long ID = 10L;
    private static final String SLUG = "osh-markazi";

    @Configuration
    @EnableCaching
    static class CacheTestConfig {
        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager("restaurants");
        }

        @Bean
        RestaurantRepository restaurantRepository() {
            return mock(RestaurantRepository.class);
        }

        @Bean
        RestaurantMapper restaurantMapper() {
            return mock(RestaurantMapper.class);
        }

        @Bean
        UserService userService() {
            return mock(UserService.class);
        }

        @Bean
        ImageStorageService imageStorageService() {
            return mock(ImageStorageService.class);
        }

        @Bean
        RestaurantService restaurantService(RestaurantRepository repo, RestaurantMapper mapper,
                                            UserService users, ImageStorageService images) {
            return new RestaurantService(repo, mapper, users, images);
        }
    }

    private AnnotationConfigApplicationContext ctx;
    private RestaurantService service;
    private RestaurantRepository repository;

    @BeforeEach
    void setUp() {
        ctx = new AnnotationConfigApplicationContext(CacheTestConfig.class);
        service = ctx.getBean(RestaurantService.class);
        repository = ctx.getBean(RestaurantRepository.class);

        Restaurant restaurant = Restaurant.builder()
                .id(ID).name("Osh Markazi").slug(SLUG)
                .status(RestaurantStatus.ACTIVE).isOpen(true)
                .owner(com.fooddelivery.auth.entity.User.builder()
                        .id(1L).status(com.fooddelivery.auth.entity.UserStatus.ACTIVE).build())
                .build();

        RestaurantDto dto = new RestaurantDto();
        dto.setId(ID);
        dto.setSlug(SLUG);

        when(repository.findById(ID)).thenReturn(Optional.of(restaurant));
        when(repository.findBySlug(SLUG)).thenReturn(Optional.of(restaurant));
        when(repository.save(any(Restaurant.class))).thenAnswer(i -> i.getArgument(0));
        when(ctx.getBean(RestaurantMapper.class).toDto(any(Restaurant.class))).thenReturn(dto);
    }

    @AfterEach
    void tearDown() {
        ctx.close();
    }

    private org.springframework.cache.Cache cache() {
        return ctx.getBean(CacheManager.class).getCache("restaurants");
    }

    /**
     * Populate both entries, and prove they landed. Asserting on the cache
     * rather than on repository call counts is deliberate: every mutating
     * method calls getRestaurantEntityById internally, so counting findById
     * gives 3 when eviction works and 2 when it does not — a test written that
     * way passes on the bug and fails on the fix.
     */
    private void warmBothCaches() {
        service.getRestaurantById(ID);
        service.getRestaurantBySlug(SLUG);

        assertThat(cache().get(ID)).as("id entry cached").isNotNull();
        assertThat(cache().get("slug:" + SLUG)).as("slug entry cached").isNotNull();
    }

    private void assertBothEvicted() {
        assertThat(cache().get(ID)).as("id entry evicted").isNull();
        assertThat(cache().get("slug:" + SLUG)).as("slug entry evicted").isNull();
    }

    @Test
    @DisplayName("toggling open/closed evicts the slug entry, not just the id entry")
    void toggleOpenEvictsBothKeys() {
        warmBothCaches();
        service.toggleOpenStatus(ID, false);
        assertBothEvicted();
    }

    @Test
    @DisplayName("changing status evicts both keys")
    void updateStatusEvictsBothKeys() {
        warmBothCaches();
        service.updateStatus(ID, RestaurantStatus.SUSPENDED);
        assertBothEvicted();
    }

    @Test
    @DisplayName("a partial update evicts both keys")
    void partialUpdateEvictsBothKeys() {
        warmBothCaches();
        service.updateRestaurantPartial(ID,
                com.fooddelivery.restaurant.dto.UpdateRestaurantRequest.builder()
                        .name("Osh Markazi 2").build());
        assertBothEvicted();
    }

    @Test
    @DisplayName("an ownership transfer evicts both keys")
    void transferOwnershipEvictsBothKeys() {
        warmBothCaches();

        com.fooddelivery.auth.entity.User newOwner = com.fooddelivery.auth.entity.User.builder()
                .id(2L).status(com.fooddelivery.auth.entity.UserStatus.ACTIVE).build();
        when(ctx.getBean(UserService.class).getUserEntityById(2L)).thenReturn(newOwner);

        service.transferOwnership(ID, 2L);
        assertBothEvicted();
    }

    @Test
    @DisplayName("the two keys really are distinct entries")
    void idAndSlugAreSeparateEntries() {
        service.getRestaurantById(ID);
        service.getRestaurantBySlug(SLUG);

        var cache = ctx.getBean(CacheManager.class).getCache("restaurants");
        assertThat(cache).isNotNull();
        assertThat(cache.get(ID)).isNotNull();
        assertThat(cache.get("slug:" + SLUG)).isNotNull();
    }
}
