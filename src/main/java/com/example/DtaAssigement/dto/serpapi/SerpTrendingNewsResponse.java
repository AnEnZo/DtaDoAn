package com.example.DtaAssigement.dto.serpapi;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SerpTrendingNewsResponse {
    private List<NewsArticle> newsArticles;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NewsArticle {
        private String title;
        private String link;
        private String source;
        private String snippet;
        private String thumbnail;
        private String date;
    }
}
