package com.example.DtaAssigement.dto.serpapi;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterestOverTime {
    private String query;
    private List<TimelineData> timelineData;
    private Integer averageValue;

    @JsonProperty("is_partial")
    private Boolean isPartial;
}
