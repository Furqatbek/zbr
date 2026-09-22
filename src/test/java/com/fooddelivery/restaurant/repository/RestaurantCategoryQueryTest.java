package com.fooddelivery.restaurant.repository;

import com.fooddelivery.auth.entity.Role;
import com.fooddelivery.auth.entity.User;
import com.fooddelivery.auth.entity.UserStatus;
import com.fooddelivery.auth.repository.UserRepository;
import com.fooddelivery.restaurant.entity.Restaurant;
import com.fooddelivery.restaurant.entity.RestaurantCategory;
import com.fooddelivery.restaurant.entity.RestaurantStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Which cuisines are offered as chips.
 *
 * <p>The rule the app asked for: never offer a chip that filters to an empty
 * list. That makes the answer depend on the restaurants behind each category
 * and on the time of day, which is a query and not something a mocked
 * repository could show.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:restcat;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false"
})
@DisplayName("RestaurantCategoryRepository.findWithOpenRestaurants")
class RestaurantCategoryQueryTest {

    @Autowired private RestaurantCategoryRepository categoryRepository;
    @Autowired private RestaurantRepository restaurantRepository;
    @Autowired private UserRepository userRepository;

    private User owner;

    @BeforeEach
    void setUp() {
        owner = userRepository.save(User.builder()
                .email("owner@example.com").passwordHash("x")
                .roles(Set.of(Role.RESTAURANT_OWNER)).status(UserStatus.ACTIVE)
                .build());
    }

    private RestaurantCategory category(String slug, String nameUz, int sortOrder) {
        return categoryRepository.save(RestaurantCategory.builder()
                .slug(slug).nameUz(nameUz).sortOrder(sortOrder).active(true).build());
    }

    private void restaurant(String name, RestaurantCategory category,
                            RestaurantStatus status, boolean open) {
        restaurantRepository.save(Restaurant.builder()
                .owner(owner).name(name).slug(name.toLowerCase().replace(' ', '-'))
                .category(category).status(status).isOpen(open)
                .build());
    }

    @Test
    @DisplayName("a category with an open restaurant is offered")
    void offeredWhenOpen() {
        RestaurantCategory burgers = category("burgers", "Burgerlar", 10);
        restaurant("Burger Place", burgers, RestaurantStatus.ACTIVE, true);

        assertThat(categoryRepository.findWithOpenRestaurants())
                .extracting(RestaurantCategory::getSlug).containsExactly("burgers");
    }

    @Test
    @DisplayName("a category with no restaurants at all is not offered")
    void emptyCategoryIsHidden() {
        // The dead-end chip: tapped, filters to nothing, and the customer has
        // learned only that the app is broken.
        category("sushi", "Sushi", 20);

        assertThat(categoryRepository.findWithOpenRestaurants()).isEmpty();
    }

    @Test
    @DisplayName("a category whose only restaurant is closed is not offered")
    void closedRestaurantHidesTheCategory() {
        RestaurantCategory coffee = category("coffee", "Kofe", 30);
        restaurant("Night Cafe", coffee, RestaurantStatus.ACTIVE, false);

        // Correct that this changes through the day: a cuisine with one venue
        // stops being offered when that venue shuts.
        assertThat(categoryRepository.findWithOpenRestaurants()).isEmpty();
    }

    @Test
    @DisplayName("a pending restaurant does not put its category on the rail")
    void pendingRestaurantDoesNotCount() {
        RestaurantCategory pizza = category("pizza", "Pitsa", 40);
        restaurant("Not Approved Yet", pizza, RestaurantStatus.PENDING, true);

        assertThat(categoryRepository.findWithOpenRestaurants()).isEmpty();
    }

    @Test
    @DisplayName("a category is listed once however many restaurants it has")
    void noDuplicates() {
        RestaurantCategory national = category("national", "Milliy taomlar", 5);
        restaurant("Osh One", national, RestaurantStatus.ACTIVE, true);
        restaurant("Osh Two", national, RestaurantStatus.ACTIVE, true);
        restaurant("Osh Three", national, RestaurantStatus.ACTIVE, true);

        assertThat(categoryRepository.findWithOpenRestaurants()).hasSize(1);
    }

    @Test
    @DisplayName("the rail comes back in sort order")
    void sortedForTheRail() {
        RestaurantCategory second = category("burgers", "Burgerlar", 20);
        RestaurantCategory first = category("national", "Milliy taomlar", 10);
        restaurant("Burger Place", second, RestaurantStatus.ACTIVE, true);
        restaurant("Osh Markazi", first, RestaurantStatus.ACTIVE, true);

        List<RestaurantCategory> rail = categoryRepository.findWithOpenRestaurants();

        assertThat(rail).extracting(RestaurantCategory::getSlug)
                .containsExactly("national", "burgers");
    }

    @Test
    @DisplayName("a deactivated category is offered to nobody, open restaurants or not")
    void inactiveCategoryIsHidden() {
        RestaurantCategory retired = category("drinks", "Ichimliklar", 50);
        retired.setActive(false);
        categoryRepository.save(retired);
        restaurant("Juice Bar", retired, RestaurantStatus.ACTIVE, true);

        assertThat(categoryRepository.findWithOpenRestaurants()).isEmpty();
    }
}
