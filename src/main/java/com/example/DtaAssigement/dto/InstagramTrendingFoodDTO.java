package com.example.DtaAssigement.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for Instagram-based Trending Food response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InstagramTrendingFoodDTO {

    private Long id;
    private String name; // Display name (e.g. "Phở")
    private String hashtag; // Hashtag (e.g. "#pho")
    private Integer rank; // Position in trending list
    private Double score; // Instagram trending score
    private EngagementMetrics engagement;
    private String trendStatus; // "🔥 VIRAL", "📈 TRENDING", etc.
    private LocalDateTime updatedAt;

    /**
     * Nested DTO for engagement metrics
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EngagementMetrics {
        private Double average; // Average engagement from top posts
        private Double recent; // Recent 24h engagement
        private Double growthRate; // Growth multiplier
        private Integer posts24h; // Number of posts in 24h
    }
}
