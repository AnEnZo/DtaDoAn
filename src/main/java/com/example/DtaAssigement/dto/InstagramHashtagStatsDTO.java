package com.example.DtaAssigement.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * Instagram Hashtag Statistics DTO
 * Contains aggregated stats from recent posts
 */
@Data
@Builder
public class InstagramHashtagStatsDTO {
    private String hashtag;
    private String hashtagId;

    // Post counts
    private Integer totalPosts; // Number of posts fetched (max 50)

    // Engagement metrics
    private Double avgLikes;
    private Double avgComments;
    private Integer totalLikes;
    private Integer totalComments;

    private LocalDateTime analyzedAt;
}
