package com.fooddelivery.analytics.cx.repository;

import com.fooddelivery.analytics.cx.model.NpsSurvey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for NPS surveys.
 */
@Repository
public interface NpsSurveyRepository extends JpaRepository<NpsSurvey, Long> {

    /**
     * Count total responses in period.
     */
    @Query("SELECT COUNT(n) FROM NpsSurvey n WHERE n.createdAt BETWEEN :start AND :end")
    Long countResponsesInPeriod(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Count promoters (score >= 9).
     */
    @Query("SELECT COUNT(n) FROM NpsSurvey n WHERE n.createdAt BETWEEN :start AND :end AND n.score >= 9")
    Long countPromoters(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Count passives (score 7-8).
     */
    @Query("SELECT COUNT(n) FROM NpsSurvey n WHERE n.createdAt BETWEEN :start AND :end AND n.score >= 7 AND n.score <= 8")
    Long countPassives(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Count detractors (score <= 6).
     */
    @Query("SELECT COUNT(n) FROM NpsSurvey n WHERE n.createdAt BETWEEN :start AND :end AND n.score <= 6")
    Long countDetractors(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Get score distribution (0-10).
     */
    @Query("SELECT n.score, COUNT(n) FROM NpsSurvey n WHERE n.createdAt BETWEEN :start AND :end " +
           "GROUP BY n.score ORDER BY n.score")
    List<Object[]> getScoreDistribution(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Get NPS by user segment.
     */
    @Query("SELECT n.userSegment, " +
           "COUNT(n), " +
           "SUM(CASE WHEN n.score >= 9 THEN 1 ELSE 0 END), " +
           "SUM(CASE WHEN n.score >= 7 AND n.score <= 8 THEN 1 ELSE 0 END), " +
           "SUM(CASE WHEN n.score <= 6 THEN 1 ELSE 0 END) " +
           "FROM NpsSurvey n WHERE n.createdAt BETWEEN :start AND :end " +
           "GROUP BY n.userSegment")
    List<Object[]> getNpsBySegment(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Get NPS by survey channel.
     */
    @Query("SELECT n.surveyChannel, " +
           "COUNT(n), " +
           "SUM(CASE WHEN n.score >= 9 THEN 1 ELSE 0 END), " +
           "SUM(CASE WHEN n.score >= 7 AND n.score <= 8 THEN 1 ELSE 0 END), " +
           "SUM(CASE WHEN n.score <= 6 THEN 1 ELSE 0 END) " +
           "FROM NpsSurvey n WHERE n.createdAt BETWEEN :start AND :end " +
           "GROUP BY n.surveyChannel")
    List<Object[]> getNpsByChannel(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Get daily NPS trend.
     */
    @Query("SELECT CAST(n.createdAt AS date), " +
           "COUNT(n), " +
           "SUM(CASE WHEN n.score >= 9 THEN 1 ELSE 0 END), " +
           "SUM(CASE WHEN n.score >= 7 AND n.score <= 8 THEN 1 ELSE 0 END), " +
           "SUM(CASE WHEN n.score <= 6 THEN 1 ELSE 0 END) " +
           "FROM NpsSurvey n WHERE n.createdAt BETWEEN :start AND :end " +
           "GROUP BY CAST(n.createdAt AS date) ORDER BY CAST(n.createdAt AS date)")
    List<Object[]> getDailyNpsTrend(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Get weekly NPS trend.
     */
    @Query("SELECT EXTRACT(YEAR FROM n.createdAt), EXTRACT(WEEK FROM n.createdAt), " +
           "COUNT(n), " +
           "SUM(CASE WHEN n.score >= 9 THEN 1 ELSE 0 END), " +
           "SUM(CASE WHEN n.score >= 7 AND n.score <= 8 THEN 1 ELSE 0 END), " +
           "SUM(CASE WHEN n.score <= 6 THEN 1 ELSE 0 END) " +
           "FROM NpsSurvey n WHERE n.createdAt BETWEEN :start AND :end " +
           "GROUP BY EXTRACT(YEAR FROM n.createdAt), EXTRACT(WEEK FROM n.createdAt) " +
           "ORDER BY EXTRACT(YEAR FROM n.createdAt), EXTRACT(WEEK FROM n.createdAt)")
    List<Object[]> getWeeklyNpsTrend(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Get surveys with comments for analysis.
     */
    @Query("SELECT n FROM NpsSurvey n WHERE n.createdAt BETWEEN :start AND :end " +
           "AND n.comment IS NOT NULL AND n.comment <> '' ORDER BY n.createdAt DESC")
    List<NpsSurvey> getSurveysWithComments(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Get surveys by score range with comments.
     */
    @Query("SELECT n FROM NpsSurvey n WHERE n.createdAt BETWEEN :start AND :end " +
           "AND n.score BETWEEN :minScore AND :maxScore " +
           "AND n.comment IS NOT NULL AND n.comment <> '' ORDER BY n.createdAt DESC")
    List<NpsSurvey> getSurveysByScoreRangeWithComments(@Param("start") LocalDateTime start,
                                                       @Param("end") LocalDateTime end,
                                                       @Param("minScore") int minScore,
                                                       @Param("maxScore") int maxScore);

    /**
     * Get average score.
     */
    @Query("SELECT AVG(n.score) FROM NpsSurvey n WHERE n.createdAt BETWEEN :start AND :end")
    Double getAverageScore(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Count follow-up requests.
     */
    @Query("SELECT COUNT(n) FROM NpsSurvey n WHERE n.createdAt BETWEEN :start AND :end " +
           "AND n.isFollowUpRequested = true")
    Long countFollowUpRequests(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
