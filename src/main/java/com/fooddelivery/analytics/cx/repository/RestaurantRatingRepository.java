package com.fooddelivery.analytics.cx.repository;

import com.fooddelivery.analytics.cx.model.RestaurantRating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for restaurant ratings.
 */
@Repository
public interface RestaurantRatingRepository extends JpaRepository<RestaurantRating, Long> {

    /**
     * Get average rating for a restaurant.
     */
    @Query("SELECT AVG(r.score) FROM RestaurantRating r WHERE r.restaurantId = :restaurantId " +
           "AND r.createdAt BETWEEN :start AND :end")
    Double getAverageRating(@Param("restaurantId") Long restaurantId,
                           @Param("start") LocalDateTime start,
                           @Param("end") LocalDateTime end);

    /**
     * Get average rating overall.
     */
    @Query("SELECT AVG(r.score) FROM RestaurantRating r WHERE r.createdAt BETWEEN :start AND :end")
    Double getOverallAverageRating(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Count ratings for a restaurant.
     */
    @Query("SELECT COUNT(r) FROM RestaurantRating r WHERE r.restaurantId = :restaurantId " +
           "AND r.createdAt BETWEEN :start AND :end")
    Long countRatings(@Param("restaurantId") Long restaurantId,
                      @Param("start") LocalDateTime start,
                      @Param("end") LocalDateTime end);

    /**
     * Count total ratings.
     */
    @Query("SELECT COUNT(r) FROM RestaurantRating r WHERE r.createdAt BETWEEN :start AND :end")
    Long countTotalRatings(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Get rating distribution for a restaurant.
     */
    @Query("SELECT r.score, COUNT(r) FROM RestaurantRating r WHERE r.restaurantId = :restaurantId " +
           "AND r.createdAt BETWEEN :start AND :end GROUP BY r.score ORDER BY r.score")
    List<Object[]> getRatingDistribution(@Param("restaurantId") Long restaurantId,
                                         @Param("start") LocalDateTime start,
                                         @Param("end") LocalDateTime end);

    /**
     * Get overall rating distribution.
     */
    @Query("SELECT r.score, COUNT(r) FROM RestaurantRating r WHERE r.createdAt BETWEEN :start AND :end " +
           "GROUP BY r.score ORDER BY r.score")
    List<Object[]> getOverallRatingDistribution(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Get sub-ratings for a restaurant.
     */
    @Query("SELECT AVG(r.foodQualityScore), AVG(r.portionSizeScore), AVG(r.valueForMoneyScore) " +
           "FROM RestaurantRating r WHERE r.restaurantId = :restaurantId " +
           "AND r.createdAt BETWEEN :start AND :end")
    Object[] getSubRatings(@Param("restaurantId") Long restaurantId,
                           @Param("start") LocalDateTime start,
                           @Param("end") LocalDateTime end);

    /**
     * Get overall sub-ratings.
     */
    @Query("SELECT AVG(r.foodQualityScore), AVG(r.portionSizeScore), AVG(r.valueForMoneyScore) " +
           "FROM RestaurantRating r WHERE r.createdAt BETWEEN :start AND :end")
    Object[] getOverallSubRatings(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Get top rated restaurants.
     */
    @Query("SELECT r.restaurantId, AVG(r.score), COUNT(r) FROM RestaurantRating r " +
           "WHERE r.createdAt BETWEEN :start AND :end " +
           "GROUP BY r.restaurantId HAVING COUNT(r) >= :minRatings ORDER BY AVG(r.score) DESC")
    List<Object[]> getTopRatedRestaurants(@Param("start") LocalDateTime start,
                                          @Param("end") LocalDateTime end,
                                          @Param("minRatings") long minRatings);

    /**
     * Get lowest rated restaurants.
     */
    @Query("SELECT r.restaurantId, AVG(r.score), COUNT(r) FROM RestaurantRating r " +
           "WHERE r.createdAt BETWEEN :start AND :end " +
           "GROUP BY r.restaurantId HAVING COUNT(r) >= :minRatings ORDER BY AVG(r.score) ASC")
    List<Object[]> getLowestRatedRestaurants(@Param("start") LocalDateTime start,
                                             @Param("end") LocalDateTime end,
                                             @Param("minRatings") long minRatings);

    /**
     * Get most reviewed restaurants.
     */
    @Query("SELECT r.restaurantId, AVG(r.score), COUNT(r) FROM RestaurantRating r " +
           "WHERE r.createdAt BETWEEN :start AND :end " +
           "GROUP BY r.restaurantId ORDER BY COUNT(r) DESC")
    List<Object[]> getMostReviewedRestaurants(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Get daily rating trend for a restaurant.
     */
    @Query("SELECT CAST(r.createdAt AS date), AVG(r.score), COUNT(r) FROM RestaurantRating r " +
           "WHERE r.restaurantId = :restaurantId AND r.createdAt BETWEEN :start AND :end " +
           "GROUP BY CAST(r.createdAt AS date) ORDER BY CAST(r.createdAt AS date)")
    List<Object[]> getDailyRatingTrend(@Param("restaurantId") Long restaurantId,
                                       @Param("start") LocalDateTime start,
                                       @Param("end") LocalDateTime end);

    /**
     * Get overall daily rating trend.
     */
    @Query("SELECT CAST(r.createdAt AS date), AVG(r.score), COUNT(r) FROM RestaurantRating r " +
           "WHERE r.createdAt BETWEEN :start AND :end " +
           "GROUP BY CAST(r.createdAt AS date) ORDER BY CAST(r.createdAt AS date)")
    List<Object[]> getOverallDailyRatingTrend(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Count ratings with comments.
     */
    @Query("SELECT COUNT(r) FROM RestaurantRating r WHERE r.restaurantId = :restaurantId " +
           "AND r.createdAt BETWEEN :start AND :end AND r.comment IS NOT NULL AND r.comment <> ''")
    Long countRatingsWithComments(@Param("restaurantId") Long restaurantId,
                                  @Param("start") LocalDateTime start,
                                  @Param("end") LocalDateTime end);

    /**
     * Count total ratings with comments.
     */
    @Query("SELECT COUNT(r) FROM RestaurantRating r WHERE r.createdAt BETWEEN :start AND :end " +
           "AND r.comment IS NOT NULL AND r.comment <> ''")
    Long countTotalRatingsWithComments(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Count restaurant responses.
     */
    @Query("SELECT COUNT(r) FROM RestaurantRating r WHERE r.restaurantId = :restaurantId " +
           "AND r.createdAt BETWEEN :start AND :end AND r.restaurantResponse IS NOT NULL")
    Long countRestaurantResponses(@Param("restaurantId") Long restaurantId,
                                  @Param("start") LocalDateTime start,
                                  @Param("end") LocalDateTime end);

    /**
     * Get ratings with comments for a restaurant.
     */
    @Query("SELECT r FROM RestaurantRating r WHERE r.restaurantId = :restaurantId " +
           "AND r.createdAt BETWEEN :start AND :end AND r.comment IS NOT NULL AND r.comment <> '' " +
           "ORDER BY r.createdAt DESC")
    List<RestaurantRating> getRatingsWithComments(@Param("restaurantId") Long restaurantId,
                                                  @Param("start") LocalDateTime start,
                                                  @Param("end") LocalDateTime end);
}
