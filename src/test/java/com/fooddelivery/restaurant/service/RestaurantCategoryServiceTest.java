package com.fooddelivery.restaurant.service;

import com.fooddelivery.common.exception.BusinessException;
import com.fooddelivery.common.exception.ResourceNotFoundException;
import com.fooddelivery.restaurant.dto.RestaurantCategoryDto;
import com.fooddelivery.restaurant.dto.SaveRestaurantCategoryRequest;
import com.fooddelivery.restaurant.entity.Restaurant;
import com.fooddelivery.restaurant.entity.RestaurantCategory;
import com.fooddelivery.restaurant.repository.RestaurantCategoryRepository;
import com.fooddelivery.restaurant.repository.RestaurantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Restaurant categories")
class RestaurantCategoryServiceTest {

    @Mock private RestaurantCategoryRepository categoryRepository;
    @Mock private RestaurantRepository restaurantRepository;

    private RestaurantCategoryService service;

    @BeforeEach
    void setUp() {
        service = new RestaurantCategoryService(categoryRepository, restaurantRepository);
        when(categoryRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(categoryRepository.existsBySlug(anyString())).thenReturn(false);
    }

    private RestaurantCategory burgers() {
        return RestaurantCategory.builder()
                .id(3L).slug("burgers")
                .nameUz("Burgerlar").nameRu("Бургеры").nameEn("Burgers")
                .imageUrl("https://zbrr.uz/media/cat/burgers.png")
                .sortOrder(30).active(true)
                .build();
    }

    @Test
    @DisplayName("the chip rail comes back in the caller's language")
    void listForCustomersIsLocalized() {
        when(categoryRepository.findWithOpenRestaurants()).thenReturn(List.of(burgers()));

        assertThat(service.listForCustomers("ru")).singleElement()
                .satisfies(c -> {
                    assertThat(c.getName()).isEqualTo("Бургеры");
                    assertThat(c.getSlug()).isEqualTo("burgers");
                    assertThat(c.getImageUrl()).endsWith("burgers.png");
                });
        assertThat(service.listForCustomers("en").get(0).getName()).isEqualTo("Burgers");
    }

    @Test
    @DisplayName("a slug is derived when none is given")
    void slugIsDerived() {
        service.create(SaveRestaurantCategoryRequest.builder()
                .nameUz("Milliy taomlar").nameEn("National food").build());

        ArgumentCaptor<RestaurantCategory> captor = ArgumentCaptor.forClass(RestaurantCategory.class);
        verify(categoryRepository).save(captor.capture());
        assertThat(captor.getValue().getSlug()).isEqualTo("national-food");
    }

    @Test
    @DisplayName("a duplicate slug is refused rather than quietly suffixed")
    void duplicateSlugIsRefused() {
        // Two categories sharing a slug would make the stable identifier
        // useless for exactly the thing it exists for.
        when(categoryRepository.existsBySlug("burgers")).thenReturn(true);

        assertThatThrownBy(() -> service.create(SaveRestaurantCategoryRequest.builder()
                .slug("burgers").nameUz("Burgerlar").build()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    @DisplayName("an update leaves out what it does not mention")
    void updateIsPartial() {
        RestaurantCategory existing = burgers();
        when(categoryRepository.findById(3L)).thenReturn(Optional.of(existing));

        service.update(3L, SaveRestaurantCategoryRequest.builder().nameRu("Бургеры и сэндвичи").build());

        assertThat(existing.getNameRu()).isEqualTo("Бургеры и сэндвичи");
        assertThat(existing.getNameUz()).isEqualTo("Burgerlar");
        assertThat(existing.getImageUrl()).endsWith("burgers.png");
    }

    @Test
    @DisplayName("renaming a category never moves its slug")
    void slugSurvivesRenames() {
        // Analytics and any deep link key on the slug. A rename that detached
        // the history would be discovered a month later, by someone reading a
        // report.
        RestaurantCategory existing = burgers();
        when(categoryRepository.findById(3L)).thenReturn(Optional.of(existing));

        service.update(3L, SaveRestaurantCategoryRequest.builder()
                .slug("something-else").nameUz("Boshqa nom").build());

        assertThat(existing.getSlug()).isEqualTo("burgers");
    }

    @Test
    @DisplayName("a restaurant can be filed, and unfiled")
    void assignAndClear() {
        Restaurant restaurant = Restaurant.builder().id(7L).name("Burger Place").build();
        when(restaurantRepository.findById(7L)).thenReturn(Optional.of(restaurant));
        when(categoryRepository.findById(3L)).thenReturn(Optional.of(burgers()));

        service.assign(7L, 3L);
        assertThat(restaurant.getCategory()).isNotNull();
        assertThat(restaurant.getCategory().getSlug()).isEqualTo("burgers");

        // Unfiling matters: a restaurant filed wrongly should not need a
        // made-up category to get out of the wrong one.
        service.assign(7L, null);
        assertThat(restaurant.getCategory()).isNull();
    }

    @Test
    @DisplayName("filing under a category that does not exist is refused")
    void unknownCategoryIsRefused() {
        when(restaurantRepository.findById(7L))
                .thenReturn(Optional.of(Restaurant.builder().id(7L).build()));
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.assign(7L, 99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("a category with only an Uzbek name still renders everywhere")
    void singleNameIsUsable() {
        RestaurantCategory sparse = RestaurantCategory.builder()
                .id(9L).slug("kabob").nameUz("Kabob").build();
        when(categoryRepository.findWithOpenRestaurants()).thenReturn(List.of(sparse));

        assertThat(service.listForCustomers("ru").get(0).getName()).isEqualTo("Kabob");
        assertThat(service.listForCustomers("en").get(0).getName()).isEqualTo("Kabob");
    }

    @Test
    @DisplayName("nothing to show is an empty rail, not a null")
    void emptyRail() {
        when(categoryRepository.findWithOpenRestaurants()).thenReturn(List.of());

        List<RestaurantCategoryDto> rail = service.listForCustomers("uz");

        assertThat(rail).isNotNull().isEmpty();
    }
}
