package com.example.DtaAssigement.dto.queue;

import com.example.DtaAssigement.dto.serpapi.SerpTrendingNowResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * Food Trend Batch Message DTO
 * Used for async batch processing of trending searches with Llama AI
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FoodTrendBatchMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    private String geo;
    private Integer batchNumber;
    private Integer totalBatches;
    private List<SerpTrendingNowResponse.TrendingSearch> trends; // Batch of up to 10 trends
}
