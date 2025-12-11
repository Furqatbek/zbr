package com.fooddelivery.analytics.cx.repository;

import com.fooddelivery.analytics.cx.model.AppStoreRating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for app store ratings.
 */
@Repository
public interface AppStoreRatingRepository extends JpaRepository<AppStoreRating, Long> {

    /**
     * Get overall average rating.
     */
    @Query("SELECT AVG(r.score) FROM AppStoreRating r WHERE r.createdAt BETWEEN :start AND :end")
    Double getOverallAverageRating(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Get average rating by platform.
     */
    @Query("SELECT AVG(r.score) FROM AppStoreRating r WHERE r.platform = :platform " +
           "AND r.createdAt BETWEEN :start AND :end")
    Double getAverageRatingByPlatform(@Param("platform") AppStoreRating.Platform platform,
                                      @Param("start") LocalDateTime start,
                                      @Param("end") LocalDateTime end);

    /**
     * Count total reviews.
     */
    @Query("SELECT COUNT(r) FROM AppStoreRating r WHERE r.createdAt BETWEEN :start AND :end")
    Long countTotalReviews(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Count reviews by platform.
     */
    @Query("SELECT COUNT(r) FROM AppStoreRating r WHERE r.platform = :platform " +
           "AND r.createdAt BETWEEN :start AND :end")
    Long countReviewsByPlatform(@Param("platform") AppStoreRating.Platform platform,
                                @Param("start") LocalDateTime start,
                                @Param("end") LocalDateTime end);

    /**
     * Get overall rating distribution.
     */
    @Query("SELECT r.score, COUNT(r) FROM AppStoreRating r WHERE r.createdAt BETWEEN :start AND :end " +
           "GROUP BY r.score ORDER BY r.score")
    List<Object[]> getOverallRatingDistribution(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Get rating distribution by platform.
     */
    @Query("SELECT r.score, COUNT(r) FROM AppStoreRating r WHERE r.platform = :platform " +
           "AND r.createdAt BETWEEN :start AND :end GROUP BY r.score ORDER BY r.score")
    List<Object[]> getRatingDistributionByPlatform(@Param("platform") AppStoreRating.Platform platform,
                                                   @Param("start") LocalDateTime start,
                                                   @Param("end") LocalDateTime end);

    /**
     * Get daily rating trend.
     */
    @Query("SELECT CAST(r.createdAt AS date), AVG(r.score), COUNT(r), " +
           "AVG(CASE WHEN r.platform = 'IOS' THEN r.score ELSE NULL END), " +
           "AVG(CASE WHEN r.platform = 'ANDROID' THEN r.score ELSE NULL END) " +
           "FROM AppStoreRating r WHERE r.createdAt BETWEEN :start AND :end " +
           "GROUP BY CAST(r.createdAt AS date) ORDER BY CAST(r.createdAt AS date)")
    List<Object[]> getDailyRatingTrend(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Get weekly rating trend.
     */
    @Query("SELECT EXTRACT(YEAR FROM r.createdAt), EXTRACT(WEEK FROM r.createdAt), " +
           "AVG(r.score), COUNT(r), " +
           "AVG(CASE WHEN r.platform = 'IOS' THEN r.score ELSE NULL END), " +
           "AVG(CASE WHEN r.platform = 'ANDROID' THEN r.score ELSE NULL END) " +
           "FROM AppStoreRating r WHERE r.createdAt BETWEEN :start AND :end " +
           "GROUP BY EXTRACT(YEAR FROM r.createdAt), EXTRACT(WEEK FROM r.createdAt) " +
           "ORDER BY EXTRACT(YEAR FROM r.createdAt), EXTRACT(WEEK FROM r.createdAt)")
    List<Object[]> getWeeklyRatingTrend(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Get ratings by app version.
     */
    @Query("SELECT r.appVersion, r.platform, AVG(r.score), COUNT(r) FROM AppStoreRating r " +
           "WHERE r.createdAt BETWEEN :start AND :end " +
           "GROUP BY r.appVersion, r.platform ORDER BY r.appVersion DESC")
    List<Object[]> getRatingsByVersion(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Get ratings by country.
     */
    @Query("SELECT r.country, AVG(r.score), COUNT(r) FROM AppStoreRating r " +
           "WHERE r.createdAt BETWEEN :start AND :end " +
           "GROUP BY r.country ORDER BY COUNT(r) DESC")
    List<Object[]> getRatingsByCountry(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Get sentiment summary.
     */
    @Query("SELECT " +
           "SUM(CASE WHEN r.sentimentScore > 0.3 THEN 1 ELSE 0 END), " +
           "SUM(CASE WHEN r.sentimentScore >= -0.3 AND r.sentimentScore <= 0.3 THEN 1 ELSE 0 END), " +
           "SUM(CASE WHEN r.sentimentScore < -0.3 THEN 1 ELSE 0 END), " +
           "AVG(r.sentimentScore) " +
           "FROM AppStoreRating r WHERE r.createdAt BETWEEN :start AND :end AND r.sentimentScore IS NOT NULL")
    Object[] getSentimentSummary(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Count reviews with developer response.
     */
    @Query("SELECT COUNT(r) FROM AppStoreRating r WHERE r.createdAt BETWEEN :start AND :end " +
           "AND r.developerResponse IS NOT NULL")
    Long countReviewsWithResponse(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Get latest version by platform.
     */
    @Query("SELECT r.appVersion FROM AppStoreRating r WHERE r.platform = :platform " +
           "ORDER BY r.createdAt DESC LIMIT 1")
    String getLatestVersion(@Param("platform") AppStoreRating.Platform platform);

    /**
     * Get reviews for sentiment analysis (with comments).
     */
    @Query("SELECT r FROM AppStoreRating r WHERE r.createdAt BETWEEN :start AND :end " +
           "AND r.comment IS NOT NULL AND r.comment <> '' ORDER BY r.createdAt DESC")
    List<AppStoreRating> getReviewsWithComments(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Get low-rated reviews for analysis.
     */
    @Query("SELECT r FROM AppStoreRating r WHERE r.createdAt BETWEEN :start AND :end " +
           "AND r.score <= 2 AND r.comment IS NOT NULL AND r.comment <> '' ORDER BY r.createdAt DESC")
    List<AppStoreRating> getLowRatedReviews(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Get high-rated reviews for analysis.
     */
    @Query("SELECT r FROM AppStoreRating r WHERE r.createdAt BETWEEN :start AND :end " +
           "AND r.score >= 4 AND r.comment IS NOT NULL AND r.comment <> '' ORDER BY r.createdAt DESC")
    List<AppStoreRating> getHighRatedReviews(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
