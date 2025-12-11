package com.fooddelivery.analytics.cx.repository;

import com.fooddelivery.analytics.cx.model.CourierRating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for courier ratings.
 */
@Repository
public interface CourierRatingRepository extends JpaRepository<CourierRating, Long> {

    /**
     * Get average rating for a courier.
     */
    @Query("SELECT AVG(r.score) FROM CourierRating r WHERE r.courierId = :courierId " +
           "AND r.createdAt BETWEEN :start AND :end")
    Double getAverageRating(@Param("courierId") Long courierId,
                           @Param("start") LocalDateTime start,
                           @Param("end") LocalDateTime end);

    /**
     * Get overall average courier rating.
     */
    @Query("SELECT AVG(r.score) FROM CourierRating r WHERE r.createdAt BETWEEN :start AND :end")
    Double getOverallAverageRating(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Count ratings for a courier.
     */
    @Query("SELECT COUNT(r) FROM CourierRating r WHERE r.courierId = :courierId " +
           "AND r.createdAt BETWEEN :start AND :end")
    Long countRatings(@Param("courierId") Long courierId,
                      @Param("start") LocalDateTime start,
                      @Param("end") LocalDateTime end);

    /**
     * Count total courier ratings.
     */
    @Query("SELECT COUNT(r) FROM CourierRating r WHERE r.createdAt BETWEEN :start AND :end")
    Long countTotalRatings(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Get rating distribution for a courier.
     */
    @Query("SELECT r.score, COUNT(r) FROM CourierRating r WHERE r.courierId = :courierId " +
           "AND r.createdAt BETWEEN :start AND :end GROUP BY r.score ORDER BY r.score")
    List<Object[]> getRatingDistribution(@Param("courierId") Long courierId,
                                         @Param("start") LocalDateTime start,
                                         @Param("end") LocalDateTime end);

    /**
     * Get overall rating distribution.
     */
    @Query("SELECT r.score, COUNT(r) FROM CourierRating r WHERE r.createdAt BETWEEN :start AND :end " +
           "GROUP BY r.score ORDER BY r.score")
    List<Object[]> getOverallRatingDistribution(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Get sub-ratings for a courier.
     */
    @Query("SELECT AVG(r.professionalismScore), AVG(r.communicationScore), AVG(r.timelinessScore) " +
           "FROM CourierRating r WHERE r.courierId = :courierId " +
           "AND r.createdAt BETWEEN :start AND :end")
    Object[] getSubRatings(@Param("courierId") Long courierId,
                           @Param("start") LocalDateTime start,
                           @Param("end") LocalDateTime end);

    /**
     * Get overall sub-ratings.
     */
    @Query("SELECT AVG(r.professionalismScore), AVG(r.communicationScore), AVG(r.timelinessScore) " +
           "FROM CourierRating r WHERE r.createdAt BETWEEN :start AND :end")
    Object[] getOverallSubRatings(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Get top rated couriers.
     */
    @Query("SELECT r.courierId, AVG(r.score), COUNT(r) FROM CourierRating r " +
           "WHERE r.createdAt BETWEEN :start AND :end " +
           "GROUP BY r.courierId HAVING COUNT(r) >= :minRatings ORDER BY AVG(r.score) DESC")
    List<Object[]> getTopRatedCouriers(@Param("start") LocalDateTime start,
                                       @Param("end") LocalDateTime end,
                                       @Param("minRatings") long minRatings);

    /**
     * Get lowest rated couriers.
     */
    @Query("SELECT r.courierId, AVG(r.score), COUNT(r) FROM CourierRating r " +
           "WHERE r.createdAt BETWEEN :start AND :end " +
           "GROUP BY r.courierId HAVING COUNT(r) >= :minRatings ORDER BY AVG(r.score) ASC")
    List<Object[]> getLowestRatedCouriers(@Param("start") LocalDateTime start,
                                          @Param("end") LocalDateTime end,
                                          @Param("minRatings") long minRatings);

    /**
     * Get most active rated couriers.
     */
    @Query("SELECT r.courierId, AVG(r.score), COUNT(r) FROM CourierRating r " +
           "WHERE r.createdAt BETWEEN :start AND :end " +
           "GROUP BY r.courierId ORDER BY COUNT(r) DESC")
    List<Object[]> getMostActiveRatedCouriers(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Get daily rating trend for a courier.
     */
    @Query("SELECT CAST(r.createdAt AS date), AVG(r.score), COUNT(r) FROM CourierRating r " +
           "WHERE r.courierId = :courierId AND r.createdAt BETWEEN :start AND :end " +
           "GROUP BY CAST(r.createdAt AS date) ORDER BY CAST(r.createdAt AS date)")
    List<Object[]> getDailyRatingTrend(@Param("courierId") Long courierId,
                                       @Param("start") LocalDateTime start,
                                       @Param("end") LocalDateTime end);

    /**
     * Get overall daily rating trend.
     */
    @Query("SELECT CAST(r.createdAt AS date), AVG(r.score), COUNT(r) FROM CourierRating r " +
           "WHERE r.createdAt BETWEEN :start AND :end " +
           "GROUP BY CAST(r.createdAt AS date) ORDER BY CAST(r.createdAt AS date)")
    List<Object[]> getOverallDailyRatingTrend(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Get tip statistics for a courier.
     */
    @Query("SELECT COUNT(r), SUM(r.tipAmount), AVG(r.tipAmount), " +
           "SUM(CASE WHEN r.tipAmount > 0 THEN 1 ELSE 0 END) " +
           "FROM CourierRating r WHERE r.courierId = :courierId " +
           "AND r.createdAt BETWEEN :start AND :end")
    Object[] getTipStats(@Param("courierId") Long courierId,
                         @Param("start") LocalDateTime start,
                         @Param("end") LocalDateTime end);

    /**
     * Get overall tip statistics.
     */
    @Query("SELECT COUNT(r), SUM(r.tipAmount), AVG(r.tipAmount), " +
           "SUM(CASE WHEN r.tipAmount > 0 THEN 1 ELSE 0 END) " +
           "FROM CourierRating r WHERE r.createdAt BETWEEN :start AND :end")
    Object[] getOverallTipStats(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Count ratings with comments.
     */
    @Query("SELECT COUNT(r) FROM CourierRating r WHERE r.courierId = :courierId " +
           "AND r.createdAt BETWEEN :start AND :end AND r.comment IS NOT NULL AND r.comment <> ''")
    Long countRatingsWithComments(@Param("courierId") Long courierId,
                                  @Param("start") LocalDateTime start,
                                  @Param("end") LocalDateTime end);

    /**
     * Count total ratings with comments.
     */
    @Query("SELECT COUNT(r) FROM CourierRating r WHERE r.createdAt BETWEEN :start AND :end " +
           "AND r.comment IS NOT NULL AND r.comment <> ''")
    Long countTotalRatingsWithComments(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
