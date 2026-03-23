package com.example.DtaAssigement.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for Instagram Media Post
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InstagramPostDTO {
    private String id;
    private String caption;
    private String mediaType;
    private String mediaUrl;
    private String permalink;
    private String timestamp;
    private Integer likeCount;
    private Integer commentsCount;
}
