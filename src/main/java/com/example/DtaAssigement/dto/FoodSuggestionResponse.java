package com.example.DtaAssigement.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for food suggestion response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FoodSuggestionResponse {
    private Long id;
    private String foodName;
    private String description;
    private String category;
    private Integer votes;
    private LocalDateTime createdAt;
    private String userEmail; // For showing who suggested (if available)
}
