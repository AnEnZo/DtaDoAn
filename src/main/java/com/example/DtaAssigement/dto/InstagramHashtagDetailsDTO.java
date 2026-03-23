package com.example.DtaAssigement.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for Instagram Hashtag details
 * Represents basic information about a hashtag from Instagram Graph API
 * 
 * Instagram API Response Format:
 * {
 * "id": "17841593698074073",
 * "name": "coke"
 * }
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InstagramHashtagDetailsDTO {

    /**
     * Hashtag ID from Instagram (static and global)
     */
    private String id;

    /**
     * Name of the hashtag (without # symbol)
     */
    private String name;
}
