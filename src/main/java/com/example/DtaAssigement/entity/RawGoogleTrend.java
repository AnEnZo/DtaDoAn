package com.example.DtaAssigement.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * Entity lưu data thô từ Google Trends / SerpAPI.
 * Được dùng làm nguồn dữ liệu để join sau khi Llama phân tích queries.
 */
@Entity
@Table(name = "raw_google_trends", uniqueConstraints = @UniqueConstraint(columnNames = { "query", "location",
        "fetched_date" }), indexes = {
                @Index(name = "idx_raw_trends_location_date", columnList = "location, fetched_date"),
                @Index(name = "idx_raw_trends_query", columnList = "query")
        })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RawGoogleTrend {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String query;

    @Column(name = "search_volume")
    private Long searchVolume;

    @Column(name = "increase_percentage")
    private Integer increasePercentage;

    /** Comma-separated categories từ SerpAPI */
    @Column(name = "categories", length = 500)
    private String categories;

    /** Related keywords / trend breakdown từ SerpAPI */
    @Column(name = "trend_breakdown", columnDefinition = "TEXT")
    private String trendBreakdown;

    @Column(length = 10)
    @Builder.Default
    private String location = "VN";

    /** Ngày fetch (dùng cho dedup check theo ngày) */
    @Column(name = "fetched_date", nullable = false)
    @Builder.Default
    private LocalDate fetchedDate = LocalDate.now();

    @Column(name = "fetched_at")
    @Builder.Default
    private LocalDateTime fetchedAt = LocalDateTime.now();

    public List<String> getCategoriesList() {
        if (categories == null || categories.isEmpty())
            return List.of();
        return Arrays.asList(categories.split(","));
    }

    public void setCategoriesList(List<String> list) {
        this.categories = (list == null || list.isEmpty()) ? null : String.join(",", list);
    }

    public List<String> getTrendBreakdownList() {
        if (trendBreakdown == null || trendBreakdown.isEmpty())
            return List.of();
        return Arrays.asList(trendBreakdown.split(","));
    }

    public void setTrendBreakdownList(List<String> list) {
        this.trendBreakdown = (list == null || list.isEmpty()) ? null : String.join(",", list);
    }
}
