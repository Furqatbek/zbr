package com.fooddelivery.analytics.cx.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entity for app store ratings from iOS App Store and Google Play.
 */
@Entity
@Table(name = "rating_app_store", indexes = {
        @Index(name = "idx_rating_app_platform", columnList = "platform"),
        @Index(name = "idx_rating_app_country", columnList = "country"),
        @Index(name = "idx_rating_app_created", columnList = "created_at"),
        @Index(name = "idx_rating_app_score", columnList = "score"),
        @Index(name = "idx_rating_app_version", columnList = "app_version")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppStoreRating {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "platform", nullable = false, length = 20)
    private Platform platform;

    @Column(name = "score", nullable = false)
    private Integer score; // 1-5

    @Column(name = "country", length = 10)
    private String country; // ISO country code (US, UZ, etc.)

    @Column(name = "app_version", length = 20)
    private String appVersion;

    @Column(name = "review_id", length = 100)
    private String reviewId; // External ID from store

    @Column(name = "author_name", length = 100)
    private String authorName;

    @Column(name = "title", length = 500)
    private String title;

    @Column(name = "comment", length = 5000)
    private String comment;

    @Column(name = "developer_response", length = 2000)
    private String developerResponse;

    @Column(name = "developer_responded_at")
    private LocalDateTime developerRespondedAt;

    @Column(name = "helpful_count")
    @Builder.Default
    private Integer helpfulCount = 0;

    @Column(name = "sentiment_score")
    private Double sentimentScore; // -1 to 1 from sentiment analysis

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "review_date")
    private LocalDateTime reviewDate; // Actual review date from store

    /**
     * Platform enum for app stores.
     */
    public enum Platform {
        IOS,
        ANDROID
    }
}
