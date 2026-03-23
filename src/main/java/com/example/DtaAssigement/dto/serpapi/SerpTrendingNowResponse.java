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
public class SerpTrendingNowResponse {
    @JsonProperty("trending_searches")
    private List<TrendingSearch> trendingSearches;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrendingSearch {
        private String query;

        @JsonProperty("start_timestamp")
        private Long startTimestamp;

        @JsonProperty("end_timestamp")
        private Long endTimestamp;

        private Boolean active;

        @JsonProperty("search_volume")
        private Integer searchVolume;

        @JsonProperty("increase_percentage")
        private Integer increasePercentage;

        private List<Category> categories;

        @JsonProperty("trend_breakdown")
        private List<String> trendBreakdown;

        @JsonProperty("serpapi_google_trends_link")
        private String serpapiGoogleTrendsLink;

        @JsonProperty("news_page_token")
        private String newsPageToken;

        @JsonProperty("serpapi_news_link")
        private String serpapiNewsLink;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Category {
        private Integer id;
        private String name;
    }
}
