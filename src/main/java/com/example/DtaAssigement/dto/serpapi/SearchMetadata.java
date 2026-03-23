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
public class SearchMetadata {
    private String id;
    private String status;

    @JsonProperty("json_endpoint")
    private String jsonEndpoint;

    @JsonProperty("created_at")
    private String createdAt;

    @JsonProperty("processed_at")
    private String processedAt;

    @JsonProperty("google_trends_url")
    private String googleTrendsUrl;

    @JsonProperty("raw_html_file")
    private String rawHtmlFile;

    @JsonProperty("prettify_html_file")
    private String prettifyHtmlFile;

    @JsonProperty("total_time_taken")
    private Double totalTimeTaken;
}
