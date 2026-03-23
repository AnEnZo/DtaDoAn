package com.example.DtaAssigement.dto.llama;

import com.example.DtaAssigement.dto.serpapi.SerpTrendingNowResponse;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

import lombok.Builder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FoodTrend {
    private String query;

    @JsonProperty("food_type")
    private String foodType; // dish|restaurant|recipe|review|event|person|other

    @JsonProperty("food_name")
    private String foodName;

    @JsonProperty("search_volume")
    private Integer searchVolume;

    @JsonProperty("increase_percentage")
    private Integer increasePercentage;

    @JsonProperty("hotness_score")
    private Integer hotnessScore;

    @JsonProperty("related_keywords")
    private List<String> relatedKeywords;

    private List<SerpTrendingNowResponse.Category> categories;

    private String reasoning;
}
