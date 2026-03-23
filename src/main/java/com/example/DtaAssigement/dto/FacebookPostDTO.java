package com.example.DtaAssigement.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for Facebook Page Post
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FacebookPostDTO {
    private String id;
    private String message;
    private String createdTime;
    private Integer reactions;
    private Integer shares;
    private Integer comments;
}
