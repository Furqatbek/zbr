package com.fooddelivery.analytics.cx.collector;

import com.fooddelivery.analytics.cx.dto.NpsMetricsDto;
import com.fooddelivery.analytics.cx.model.NpsSurvey;
import com.fooddelivery.analytics.cx.repository.NpsSurveyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Collector for NPS (Net Promoter Score) metrics.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NpsCollector {

    private final NpsSurveyRepository npsSurveyRepository;

    /**
     * Calculate NPS metrics for a period.
     *
     * NPS Formula:
     * - Promoters: score >= 9
     * - Passives: score 7-8
     * - Detractors: score <= 6
     * - NPS = ((promoters - detractors) / total) * 100
     */
    public NpsMetricsDto collect(LocalDateTime startDate, LocalDateTime endDate,
                                 boolean includeDistribution, boolean includeTrend,
                                 boolean includeSegments) {
        log.debug("Collecting NPS metrics from {} to {}", startDate, endDate);

        // Get core counts
        Long totalResponses = npsSurveyRepository.countResponsesInPeriod(startDate, endDate);
        Long promotersCount = npsSurveyRepository.countPromoters(startDate, endDate);
        Long passivesCount = npsSurveyRepository.countPassives(startDate, endDate);
        Long detractorsCount = npsSurveyRepository.countDetractors(startDate, endDate);

        if (totalResponses == null) totalResponses = 0L;
        if (promotersCount == null) promotersCount = 0L;
        if (passivesCount == null) passivesCount = 0L;
        if (detractorsCount == null) detractorsCount = 0L;

        // Calculate NPS score
        Double npsScore = calculateNpsScore(promotersCount, detractorsCount, totalResponses);

        // Calculate percentages
        Double promotersPercentage = totalResponses > 0 ? (promotersCount * 100.0 / totalResponses) : 0.0;
        Double passivesPercentage = totalResponses > 0 ? (passivesCount * 100.0 / totalResponses) : 0.0;
        Double detractorsPercentage = totalResponses > 0 ? (detractorsCount * 100.0 / totalResponses) : 0.0;

        NpsMetricsDto.NpsMetricsDtoBuilder builder = NpsMetricsDto.builder()
                .npsScore(npsScore)
                .totalResponses(totalResponses)
                .promotersCount(promotersCount)
                .passivesCount(passivesCount)
                .detractorsCount(detractorsCount)
                .promotersPercentage(promotersPercentage)
                .passivesPercentage(passivesPercentage)
                .detractorsPercentage(detractorsPercentage)
                .periodStart(startDate)
                .periodEnd(endDate);

        // Score distribution (0-10)
        if (includeDistribution) {
            Map<Integer, Long> scoreDistribution = getScoreDistribution(startDate, endDate);
            builder.scoreDistribution(scoreDistribution);
        }

        // NPS by segment
        if (includeSegments) {
            Map<String, NpsMetricsDto.SegmentNpsDto> npsBySegment = getNpsBySegment(startDate, endDate);
            Map<String, NpsMetricsDto.SegmentNpsDto> npsByChannel = getNpsByChannel(startDate, endDate);
            builder.npsBySegment(npsBySegment);
            builder.npsByChannel(npsByChannel);
        }

        // NPS trend
        if (includeTrend) {
            List<NpsMetricsDto.NpsTrendDto> npsTrend = getDailyNpsTrend(startDate, endDate);
            builder.npsTrend(npsTrend);
        }

        return builder.build();
    }

    /**
     * Calculate NPS score from promoters, detractors, and total.
     * NPS = ((promoters - detractors) / total) * 100
     */
    public Double calculateNpsScore(Long promoters, Long detractors, Long total) {
        if (total == null || total == 0) {
            return 0.0;
        }
        return ((promoters - detractors) * 100.0) / total;
    }

    /**
     * Get score distribution (0-10).
     */
    public Map<Integer, Long> getScoreDistribution(LocalDateTime startDate, LocalDateTime endDate) {
        Map<Integer, Long> distribution = new LinkedHashMap<>();
        // Initialize all scores 0-10
        for (int i = 0; i <= 10; i++) {
            distribution.put(i, 0L);
        }

        npsSurveyRepository.getScoreDistribution(startDate, endDate).forEach(row -> {
            Integer score = ((Number) row[0]).intValue();
            Long count = ((Number) row[1]).longValue();
            distribution.put(score, count);
        });

        return distribution;
    }

    /**
     * Get NPS breakdown by user segment.
     */
    public Map<String, NpsMetricsDto.SegmentNpsDto> getNpsBySegment(LocalDateTime startDate, LocalDateTime endDate) {
        Map<String, NpsMetricsDto.SegmentNpsDto> segmentNps = new HashMap<>();

        npsSurveyRepository.getNpsBySegment(startDate, endDate).forEach(row -> {
            String segment = row[0] != null ? (String) row[0] : "UNKNOWN";
            Long count = ((Number) row[1]).longValue();
            Long promoters = ((Number) row[2]).longValue();
            Long passives = ((Number) row[3]).longValue();
            Long detractors = ((Number) row[4]).longValue();
            Double nps = calculateNpsScore(promoters, detractors, count);

            segmentNps.put(segment, NpsMetricsDto.SegmentNpsDto.builder()
                    .segment(segment)
                    .npsScore(nps)
                    .responseCount(count)
                    .promoters(promoters)
                    .passives(passives)
                    .detractors(detractors)
                    .build());
        });

        return segmentNps;
    }

    /**
     * Get NPS breakdown by survey channel.
     */
    public Map<String, NpsMetricsDto.SegmentNpsDto> getNpsByChannel(LocalDateTime startDate, LocalDateTime endDate) {
        Map<String, NpsMetricsDto.SegmentNpsDto> channelNps = new HashMap<>();

        npsSurveyRepository.getNpsByChannel(startDate, endDate).forEach(row -> {
            String channel = row[0] != null ? ((NpsSurvey.SurveyChannel) row[0]).name() : "UNKNOWN";
            Long count = ((Number) row[1]).longValue();
            Long promoters = ((Number) row[2]).longValue();
            Long passives = ((Number) row[3]).longValue();
            Long detractors = ((Number) row[4]).longValue();
            Double nps = calculateNpsScore(promoters, detractors, count);

            channelNps.put(channel, NpsMetricsDto.SegmentNpsDto.builder()
                    .segment(channel)
                    .npsScore(nps)
                    .responseCount(count)
                    .promoters(promoters)
                    .passives(passives)
                    .detractors(detractors)
                    .build());
        });

        return channelNps;
    }

    /**
     * Get daily NPS trend.
     */
    public List<NpsMetricsDto.NpsTrendDto> getDailyNpsTrend(LocalDateTime startDate, LocalDateTime endDate) {
        return npsSurveyRepository.getDailyNpsTrend(startDate, endDate).stream()
                .map(row -> {
                    LocalDateTime date = ((java.sql.Date) row[0]).toLocalDate().atStartOfDay();
                    Long count = ((Number) row[1]).longValue();
                    Long promoters = ((Number) row[2]).longValue();
                    Long passives = ((Number) row[3]).longValue();
                    Long detractors = ((Number) row[4]).longValue();
                    Double nps = calculateNpsScore(promoters, detractors, count);

                    return NpsMetricsDto.NpsTrendDto.builder()
                            .date(date)
                            .npsScore(nps)
                            .responseCount(count)
                            .promoters(promoters)
                            .passives(passives)
                            .detractors(detractors)
                            .build();
                })
                .collect(Collectors.toList());
    }

    /**
     * Extract common feedback themes from NPS comments.
     * This is a simplified version - in production would use NLP.
     */
    public List<NpsMetricsDto.FeedbackThemeDto> extractFeedbackThemes(LocalDateTime startDate, LocalDateTime endDate) {
        List<NpsSurvey> surveysWithComments = npsSurveyRepository.getSurveysWithComments(startDate, endDate);

        // Simple keyword-based theme extraction
        Map<String, List<NpsSurvey>> themeMap = new HashMap<>();
        Map<String, String[]> themeKeywords = Map.of(
                "delivery_speed", new String[]{"fast", "quick", "slow", "late", "delay"},
                "food_quality", new String[]{"food", "taste", "fresh", "cold", "quality"},
                "app_usability", new String[]{"app", "easy", "confusing", "bug", "crash"},
                "customer_service", new String[]{"support", "help", "response", "service"},
                "pricing", new String[]{"price", "expensive", "cheap", "cost", "value"}
        );

        for (NpsSurvey survey : surveysWithComments) {
            String comment = survey.getComment().toLowerCase();
            for (Map.Entry<String, String[]> entry : themeKeywords.entrySet()) {
                for (String keyword : entry.getValue()) {
                    if (comment.contains(keyword)) {
                        themeMap.computeIfAbsent(entry.getKey(), k -> new ArrayList<>()).add(survey);
                        break;
                    }
                }
            }
        }

        return themeMap.entrySet().stream()
                .map(entry -> {
                    List<NpsSurvey> surveys = entry.getValue();
                    double avgScore = surveys.stream()
                            .mapToInt(NpsSurvey::getScore)
                            .average()
                            .orElse(0.0);

                    String sentiment = avgScore >= 7 ? "POSITIVE" : (avgScore >= 5 ? "NEUTRAL" : "NEGATIVE");

                    List<String> sampleComments = surveys.stream()
                            .limit(3)
                            .map(NpsSurvey::getComment)
                            .collect(Collectors.toList());

                    return NpsMetricsDto.FeedbackThemeDto.builder()
                            .theme(entry.getKey())
                            .mentionCount((long) surveys.size())
                            .averageScore(avgScore)
                            .sentiment(sentiment)
                            .sampleComments(sampleComments)
                            .build();
                })
                .sorted((a, b) -> Long.compare(b.getMentionCount(), a.getMentionCount()))
                .collect(Collectors.toList());
    }
}
