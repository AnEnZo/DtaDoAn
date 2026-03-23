package com.example.DtaAssigement.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for YouTube Search Result (lightweight, from search.list API)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class YouTubeSearchResultDTO {
    private String videoId;
    private String title;
    private String description;
    private String channelTitle;
    private String channelId;
    private String publishedAt;
    private String thumbnailUrl;
}
