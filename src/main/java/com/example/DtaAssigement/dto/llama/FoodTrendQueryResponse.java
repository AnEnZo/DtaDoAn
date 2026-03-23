package com.example.DtaAssigement.dto.llama;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO cho endpoint POST /api/v1/analyze-food-trends của Llama Python
 * service.
 * Chỉ chứa query và reasoning — đơn giản và gọn nhẹ.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FoodTrendQueryResponse {

    @JsonProperty("total_food_trends")
    private Integer totalFoodTrends;

    @JsonProperty("food_trends")
    private List<FoodTrendQueryItem> foodTrends;

}
