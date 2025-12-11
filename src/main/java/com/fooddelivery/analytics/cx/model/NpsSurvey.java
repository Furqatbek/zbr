package com.fooddelivery.analytics.cx.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entity for Net Promoter Score (NPS) surveys.
 */
@Entity
@Table(name = "nps_surveys", indexes = {
        @Index(name = "idx_nps_user", columnList = "user_id"),
        @Index(name = "idx_nps_created", columnList = "created_at"),
        @Index(name = "idx_nps_score", columnList = "score"),
        @Index(name = "idx_nps_channel", columnList = "survey_channel")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NpsSurvey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "score", nullable = false)
    private Integer score; // 0-10 (NPS scale)

    @Column(name = "comment", length = 2000)
    private String comment;

    @Enumerated(EnumType.STRING)
    @Column(name = "survey_channel", length = 20)
    private SurveyChannel surveyChannel;

    @Column(name = "survey_trigger", length = 50)
    private String surveyTrigger; // What triggered the survey (e.g., "post_order", "app_update", "periodic")

    @Column(name = "order_id")
    private Long orderId; // If survey was triggered after an order

    @Column(name = "user_segment", length = 50)
    private String userSegment; // e.g., "new_user", "loyal", "churned"

    @Column(name = "app_version", length = 20)
    private String appVersion;

    @Column(name = "device_type", length = 50)
    private String deviceType;

    @Column(name = "is_follow_up_requested")
    @Builder.Default
    private Boolean isFollowUpRequested = false;

    @Column(name = "follow_up_completed_at")
    private LocalDateTime followUpCompletedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Survey channel enum.
     */
    public enum SurveyChannel {
        IN_APP,
        EMAIL,
        SMS,
        PUSH_NOTIFICATION,
        WEB
    }

    /**
     * Check if user is a promoter (score 9-10).
     */
    public boolean isPromoter() {
        return score != null && score >= 9;
    }

    /**
     * Check if user is a passive (score 7-8).
     */
    public boolean isPassive() {
        return score != null && score >= 7 && score <= 8;
    }

    /**
     * Check if user is a detractor (score 0-6).
     */
    public boolean isDetractor() {
        return score != null && score <= 6;
    }
}
