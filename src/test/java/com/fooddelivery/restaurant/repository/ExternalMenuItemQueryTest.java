package com.fooddelivery.restaurant.repository;

import com.fooddelivery.auth.entity.Role;
import com.fooddelivery.auth.entity.User;
import com.fooddelivery.auth.entity.UserStatus;
import com.fooddelivery.restaurant.entity.MenuCategory;
import com.fooddelivery.restaurant.entity.MenuItem;
import com.fooddelivery.restaurant.entity.Restaurant;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Which items a Restos sync is allowed to consider for retirement.
 *
 * <p>This query decides what the deletion pass can touch, and it is a string —
 * a mocked repository returns whatever the test says, so only a real database
 * proves the {@code externalId IS NOT NULL} clause is actually there. Get it
 * wrong and a sync silently deactivates every dish the restaurant added by
 * hand, because Restos has never heard of any of them.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:extmenu;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false"
})
@DisplayName("Externally-sourced menu items")
class ExternalMenuItemQueryTest {

    private static final String SOURCE = "RESTOS";

    @Autowired private MenuItemRepository menuItemRepository;
    @Autowired private EntityManager em;

    private MenuCategory category;
    private Restaurant restaurant;

    @BeforeEach
    void setUp() {
        User owner = User.builder()
                .phone("998901234567").role(Role.RESTAURANT_OWNER).status(UserStatus.ACTIVE).build();
        em.persist(owner);

        restaurant = Restaurant.builder().owner(owner).name("Osh Markazi").slug("osh-markazi").build();
        em.persist(restaurant);

        category = MenuCategory.builder().restaurant(restaurant).name("Main").build();
        em.persist(category);
    }

    private MenuItem item(String name, Long externalId, String source, boolean active) {
        MenuItem item = MenuItem.builder()
                .category(category).name(name).price(new BigDecimal("15000"))
                .externalId(externalId).externalSource(source).active(active).build();
        em.persist(item);
        return item;
    }

    @Test
    @DisplayName("an item the restaurant created by hand is never a candidate")
    void handMadeItemsExcluded() {
        // No external id and no source, so it was never in the Restos menu and
        // its absence from a snapshot means nothing.
        item("House special", null, null, true);
        MenuItem imported = item("Plov", 500L, SOURCE, true);
        em.flush();

        assertThat(menuItemRepository.findActiveExternalItems(restaurant.getId(), SOURCE))
                .extracting(MenuItem::getId)
                .containsExactly(imported.getId());
    }

    @Test
    @DisplayName("a Restos item carrying no external id is never a candidate")
    void sourcedButUnkeyedItemsExcluded() {
        // THE case the IS NOT NULL clause exists for, and the one the test above
        // does NOT cover — there the source filter alone would do the work.
        //
        // The import stamps externalSource on everything it creates, including a
        // product that arrived without an id. Such a row can never appear in a
        // snapshot, because there is no id to record, so a query that returned
        // it would deactivate it on every single sync, forever.
        item("Imported without an id", null, SOURCE, true);
        em.flush();

        assertThat(menuItemRepository.findActiveExternalItems(restaurant.getId(), SOURCE)).isEmpty();
    }

    @Test
    @DisplayName("an item from a different external system is not a candidate")
    void otherSourcesExcluded() {
        item("From another POS", 500L, "OTHER_POS", true);
        em.flush();

        assertThat(menuItemRepository.findActiveExternalItems(restaurant.getId(), SOURCE)).isEmpty();
    }

    @Test
    @DisplayName("an already-deactivated item is not returned again")
    void inactiveItemsExcluded() {
        // Otherwise every sync would re-count the same retired dishes and push
        // the deactivation total over the safety limit for no reason.
        item("Retired", 500L, SOURCE, false);
        em.flush();

        assertThat(menuItemRepository.findActiveExternalItems(restaurant.getId(), SOURCE)).isEmpty();
    }

    @Test
    @DisplayName("another restaurant's items are never a candidate")
    void otherRestaurantsExcluded() {
        User otherOwner = User.builder()
                .phone("998901234568").role(Role.RESTAURANT_OWNER).status(UserStatus.ACTIVE).build();
        em.persist(otherOwner);
        Restaurant other = Restaurant.builder().owner(otherOwner).name("Other").slug("other").build();
        em.persist(other);
        MenuCategory otherCategory = MenuCategory.builder().restaurant(other).name("Main").build();
        em.persist(otherCategory);
        em.persist(MenuItem.builder().category(otherCategory).name("Their dish")
                .price(new BigDecimal("15000")).externalId(500L).externalSource(SOURCE).active(true).build());

        item("Ours", 501L, SOURCE, true);
        em.flush();

        // Restos product ids are only unique within a venue, so a sync for one
        // restaurant must not see another's rows at all.
        assertThat(menuItemRepository.findActiveExternalItems(restaurant.getId(), SOURCE))
                .extracting(MenuItem::getName)
                .containsExactly("Ours");
    }

    @Test
    @DisplayName("live imported items are returned")
    void importedItemsReturned() {
        item("Plov", 500L, SOURCE, true);
        item("Lagman", 501L, SOURCE, true);
        em.flush();

        assertThat(menuItemRepository.findActiveExternalItems(restaurant.getId(), SOURCE)).hasSize(2);
    }

    @Test
    @DisplayName("looking up a null external id matches an unkeyed row rather than missing")
    void nullExternalIdLookupMatches() {
        // The fact the import fix rests on, and the opposite of the intuition
        // that "null matches nothing because NULL != NULL in SQL". Spring Data
        // renders a null parameter on a derived query as IS NULL, so an unkeyed
        // product does not fall through to the create branch — it lands on the
        // first unkeyed row in the category and overwrites it.
        //
        // That is why the import now refuses products without an id instead of
        // trying to store them. If this ever starts returning empty, the
        // reasoning in RestosMenuImportService needs revisiting.
        MenuItem unkeyed = item("Imported without an id", null, SOURCE, true);
        em.flush();

        assertThat(menuItemRepository
                .findByCategoryIdAndExternalSourceAndExternalId(category.getId(), SOURCE, null))
                .contains(unkeyed);
    }

    @Test
    @DisplayName("stranded unkeyed rows are counted so a sync can report them")
    void unkeyedRowsCounted() {
        item("Stranded", null, SOURCE, true);
        item("Also stranded", null, SOURCE, true);
        item("Fine", 500L, SOURCE, true);
        item("Retired stranded", null, SOURCE, false);
        item("The restaurant's own", null, null, true);
        em.flush();

        assertThat(menuItemRepository.countUnkeyedExternalItems(restaurant.getId(), SOURCE)).isEqualTo(2);
    }

    @Test
    @DisplayName("a list of candidates is empty rather than null when nothing matches")
    void emptyWhenNothingImported() {
        item("House special", null, null, true);
        em.flush();

        assertThat(menuItemRepository.findActiveExternalItems(restaurant.getId(), SOURCE)).isEmpty();
    }
}
