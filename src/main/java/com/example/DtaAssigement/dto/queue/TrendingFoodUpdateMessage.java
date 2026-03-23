package com.example.DtaAssigement.dto.queue;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Trending Food Update Message DTO
 * Used for async trending food update processing
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrendingFoodUpdateMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    private String geo;
    private String timeWindow;
    private String category;
    private boolean clearCache;
}
