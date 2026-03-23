package com.example.DtaAssigement.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for YouTube Video Category
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class YouTubeCategoryDTO {
    private String id;
    private String title;
    private Boolean assignable;
}
