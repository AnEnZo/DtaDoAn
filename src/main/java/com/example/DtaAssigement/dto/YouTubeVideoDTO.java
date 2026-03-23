package com.example.DtaAssigement.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for YouTube Video with full details (snippet + statistics +
 * contentDetails)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class YouTubeVideoDTO {
    private String id;
    private String title;
    private String description;
    private String channelTitle;
    private String channelId;
    private String thumbnailUrl;
    private String publishedAt;

    // Statistics
    private Long viewCount;
    private Long likeCount;
    private Long commentCount;

    // Content Details
    private String duration;
    private String definition; // "hd" or "sd"
    private String categoryId;
}
