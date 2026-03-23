package com.example.DtaAssigement.dto.searchapi;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * Response model for Google Trends Trending News API
 */
@Data
public class TrendingNewsResponse {

    @JsonProperty("search_metadata")
    private SearchMetadata searchMetadata;

    @JsonProperty("articles")
    private List<NewsArticle> articles;

    @Data
    public static class SearchMetadata {
        private String id;
        private String status;
    }

    @Data
    public static class NewsArticle {
        private String title;
        private String source;
        private String url;

        @JsonProperty("thumbnail")
        private String thumbnailUrl;

        @JsonProperty("time_ago")
        private String timeAgo;
    }
}
