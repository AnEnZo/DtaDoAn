package com.example.DtaAssigement.dto.foodtrends;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FoodTrendsResponse {

    @JsonProperty("total_trends")
    private Integer totalTrends;

    private String geo;

    @JsonProperty("time_range")
    private String timeRange; // "24 hours", "7 days", etc.

    @JsonProperty("data_source")
    private String dataSource; // "serpapi_llama_ai" or "database"

    @JsonProperty("saved_to_database")
    private Boolean savedToDatabase;

    private LocalDate date; // For historical data

    private List<FoodTrendItemDTO> trends;
}
