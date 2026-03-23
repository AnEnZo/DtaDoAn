package com.example.DtaAssigement.dto.serpapi;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchParameters {
    private String engine;
    private String q;
    private String geo;
    private String date;
    private String hl;

    @JsonProperty("data_type")
    private String dataType;

    private Integer tz;
}
