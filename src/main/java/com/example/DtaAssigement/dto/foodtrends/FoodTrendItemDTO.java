package com.example.DtaAssigement.dto.foodtrends;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FoodTrendItemDTO {

    @JsonProperty("food_name")
    private String foodName;

    @JsonProperty("food_type")
    private String foodType; // dish, restaurant, drink, recipe, etc.

    @JsonProperty("hotness_score")
    private Integer hotnessScore; // 1-10

    @JsonProperty("search_volume")
    private Long searchVolume;

    @JsonProperty("increase_percentage")
    private Integer increasePercentage;

    @JsonProperty("related_keywords")
    private List<String> relatedKeywords;

    private String reasoning;

    @JsonProperty("first_seen_at")
    private LocalDateTime firstSeenAt;

    @JsonProperty("last_updated_at")
    private LocalDateTime lastUpdatedAt;

    private String status; // ACTIVE, DECLINING, ARCHIVED
}
