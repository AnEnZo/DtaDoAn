package com.example.DtaAssigement.dto.foodtrends;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RawTrendItemDTO {
    private Long id;
    private String query;
    
    @JsonProperty("search_volume")
    private Long searchVolume;
    
    @JsonProperty("increase_percentage")
    private Integer increasePercentage;
    
    private String categories;
    
    @JsonProperty("trend_breakdown")
    private String trendBreakdown;
    
    private String location;
    
    @JsonProperty("fetched_date")
    private LocalDate fetchedDate;
    
    @JsonProperty("fetched_at")
    private LocalDateTime fetchedAt;
}
