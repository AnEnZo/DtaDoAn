package com.example.DtaAssigement.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for Trending Food data
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrendingFoodDTO {

    private Long id;
    private String query;
    private Long searchVolume;
    private Integer percentageIncrease;
    private Integer trendScore;
    private List<String> categories;
    private String newsToken;
    private String location;
    private LocalDateTime firstSeenAt;
    private LocalDateTime lastUpdatedAt;
    private String status;

    // Optional: Menu item if linked
    private Long menuItemId;
    private String menuItemName;
}
