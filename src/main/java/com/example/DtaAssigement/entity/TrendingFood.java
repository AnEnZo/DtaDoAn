package com.example.DtaAssigement.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * Entity representing a trending food/beverage from Google Trends
 */
@Entity
@Table(name = "trending_foods", uniqueConstraints = @UniqueConstraint(columnNames = { "query",
        "location" }), indexes = {
                @Index(name = "idx_trending_foods_status", columnList = "status"),
                @Index(name = "idx_trending_foods_score", columnList = "trend_score DESC"),
                @Index(name = "idx_trending_foods_updated", columnList = "last_updated_at DESC"),
                @Index(name = "idx_trending_foods_location", columnList = "location")
        })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrendingFood {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String query;

    @Column(name = "search_volume")
    private Long searchVolume;

    @Column(name = "percentage_increase")
    private Integer percentageIncrease;

    @Column(name = "trend_score")
    @Builder.Default
    private Integer trendScore = 0;

    // ===== Instagram-specific fields =====

    @Column(name = "instagram_hashtag_id", length = 50)
    private String instagramHashtagId;

    @Column(name = "avg_engagement")
    private Double avgEngagement; // Average engagement from top posts

    @Column(name = "recent_engagement")
    private Double recentEngagement; // Average engagement from recent 24h posts

    @Column(name = "growth_rate")
    private Double growthRate; // recentEngagement / avgEngagement

    @Column(name = "instagram_score")
    private Double instagramScore; // Final trending score based on Instagram data

    @Column(name = "total_posts_24h")
    private Integer totalPosts24h; // Number of posts in last 24 hours

    @Column(name = "data_source", length = 20)
    @Builder.Default
    private String dataSource = "GOOGLE_TRENDS"; // GOOGLE_TRENDS, INSTAGRAM, BOTH

    // Store as comma-separated string instead of array
    @Column(name = "categories", length = 500)
    private String categories;

    /**
     * Get categories as List
     */
    public List<String> getCategoriesList() {
        if (categories == null || categories.isEmpty()) {
            return List.of();
        }
        return Arrays.asList(categories.split(","));
    }

    /**
     * Set categories from List
     */
    public void setCategoriesList(List<String> categoriesList) {
        if (categoriesList == null || categoriesList.isEmpty()) {
            this.categories = null;
        } else {
            this.categories = String.join(",", categoriesList);
        }
    }

    @Column(name = "news_token", columnDefinition = "TEXT")
    private String newsToken;

    @Column(length = 10)
    @Builder.Default
    private String location = "VN";

    @Column(name = "first_seen_at")
    @Builder.Default
    private LocalDateTime firstSeenAt = LocalDateTime.now();

    @Column(name = "last_updated_at")
    @Builder.Default
    private LocalDateTime lastUpdatedAt = LocalDateTime.now();

    @Column(length = 50)
    @Builder.Default
    private String status = "ACTIVE"; // ACTIVE, DECLINING, ARCHIVED

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "menu_item_id")
    private MenuItem menuItem;

    /**
     * Calculate trend score based on search volume and percentage increase (Google
     * Trends)
     */
    public void calculateTrendScore() {
        if (searchVolume == null || percentageIncrease == null) {
            this.trendScore = 0;
            return;
        }

        // Score formula: (search_volume / 1000) * 0.4 + (percentage_increase) * 0.6
        double volumeScore = (searchVolume / 1000.0) * 0.4;
        double increaseScore = percentageIncrease * 0.6;

        this.trendScore = (int) (volumeScore + increaseScore);
    }

    /**
     * Calculate Instagram trending score based on engagement and growth rate
     * Formula: (avgEngagement × 0.4) + (growthRate × 1000 × 0.6)
     * 
     * Weight breakdown:
     * - 40% average engagement (stability)
     * - 60% growth rate (viral potential)
     */
    public void calculateInstagramScore() {
        if (avgEngagement == null || growthRate == null) {
            this.instagramScore = 0.0;
            return;
        }

        double engagementScore = avgEngagement * 0.4;
        double growthScore = growthRate * 1000 * 0.6;

        this.instagramScore = engagementScore + growthScore;
    }

    /**
     * Update last updated timestamp
     */
    @PreUpdate
    public void preUpdate() {
        this.lastUpdatedAt = LocalDateTime.now();
    }
}
