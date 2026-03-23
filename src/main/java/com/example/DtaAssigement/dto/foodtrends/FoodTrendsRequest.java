package com.example.DtaAssigement.dto.foodtrends;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FoodTrendsRequest {

    @Builder.Default
    private String geo = "VN";

    @Builder.Default
    private Integer hours = 24;

    @Builder.Default
    private Integer limit = 10;

    private String category; // Optional: dish, drink, restaurant, etc.
}
