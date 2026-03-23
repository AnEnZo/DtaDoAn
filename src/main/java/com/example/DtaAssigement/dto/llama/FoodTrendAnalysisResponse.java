package com.example.DtaAssigement.dto.llama;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FoodTrendAnalysisResponse {
    @JsonProperty("total_food_trends")
    private Integer totalFoodTrends;

    @JsonProperty("food_trends")
    private List<FoodTrend> foodTrends;
}
