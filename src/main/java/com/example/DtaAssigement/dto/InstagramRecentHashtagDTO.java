package com.example.DtaAssigement.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for Instagram recently searched hashtags
 * Represents hashtags that the Instagram Business Account has searched in the
 * last 7 days
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InstagramRecentHashtagDTO {

    /**
     * Hashtag ID from Instagram Graph API
     * This is the only field returned by Instagram's recently_searched_hashtags
     * endpoint
     */
    private String id;
}
