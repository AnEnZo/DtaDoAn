package com.example.DtaAssigement.dto.searchapi;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * Response model for Google Trends Trending Now API
 */
@Data
public class TrendingNowResponse {

    @JsonProperty("search_metadata")
    private SearchMetadata searchMetadata;

    @JsonProperty("search_parameters")
    private SearchParameters searchParameters;

    @JsonProperty("trends")
    private List<TrendTopic> trends;

    @Data
    public static class SearchMetadata {
        private String id;
        private String status;
        @JsonProperty("created_at")
        private String createdAt;
    }

    @Data
    public static class SearchParameters {
        private String engine;
        private String geo;
        private String time;
        private String hl;
    }

    @Data
    public static class TrendTopic {
        private Integer position;
        private String query;

        @JsonProperty("search_volume")
        private Long searchVolume;

        @JsonProperty("percentage_increase")
        private Integer percentageIncrease;

        private String location;
        private List<String> categories;

        @JsonProperty("start_date")
        private String startDate;

        @JsonProperty("is_active")
        private Boolean isActive;

        private List<String> keywords;

        @JsonProperty("news_token")
        private String newsToken;
    }
}
