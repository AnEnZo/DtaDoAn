package com.example.DtaAssigement.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for YouTube Comment
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class YouTubeCommentDTO {
    private String commentId;
    private String authorName;
    private String authorChannelId;
    private String textDisplay;
    private Integer likeCount;
    private String publishedAt;
    private Integer replyCount;
}
