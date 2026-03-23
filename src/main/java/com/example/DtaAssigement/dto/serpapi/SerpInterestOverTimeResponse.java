package com.example.DtaAssigement.dto.serpapi;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SerpInterestOverTimeResponse {
    @JsonProperty("search_metadata")
    private SearchMetadata searchMetadata;

    @JsonProperty("search_parameters")
    private SearchParameters searchParameters;

    @JsonProperty("interest_over_time")
    private List<InterestOverTime> interestOverTime;
}
