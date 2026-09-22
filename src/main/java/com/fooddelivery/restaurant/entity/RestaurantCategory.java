package com.fooddelivery.restaurant.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * A cuisine — the chips above the restaurant list.
 *
 * <p>Not {@link MenuCategory}, which groups dishes inside one restaurant. This
 * groups restaurants, and the two share nothing but the word.
 *
 * <p>Names are held per language because the app renders what we send,
 * verbatim, and ships in three. One name would be wrong for two thirds of its
 * customers.
 */
@Entity
@Table(name = "restaurant_categories", indexes = {
        @Index(name = "idx_restaurant_categories_active", columnList = "active, sort_order")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RestaurantCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Stable across renames: what analytics and deep links should key on. */
    @Column(nullable = false, unique = true, length = 80)
    private String slug;

    @Column(name = "name_uz", nullable = false, length = 120)
    private String nameUz;

    @Column(name = "name_ru", length = 120)
    private String nameRu;

    @Column(name = "name_en", length = 120)
    private String nameEn;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * The name to render for a language, falling back rather than rendering
     * blank.
     *
     * <p>Uzbek is the backstop because it is the only one the schema requires:
     * a category added in a hurry with one name is still usable everywhere,
     * which is better than a chip with no label on it.
     */
    public String nameFor(String language) {
        return com.fooddelivery.common.i18n.LocalizedName.pick(language, nameUz, nameRu, nameEn);
    }
}
