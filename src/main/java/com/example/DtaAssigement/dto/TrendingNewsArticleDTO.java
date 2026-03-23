package com.example.DtaAssigement.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for Trending News Article
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrendingNewsArticleDTO {

    private String title;
    private String source;
    private String url;
    private String thumbnailUrl;
    private String timeAgo;
}
