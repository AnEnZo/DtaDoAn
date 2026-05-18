package com.example.DtaAssigement.dto.foodtrends;

import com.example.DtaAssigement.dto.common.PagedResponse;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Response trả về raw trends từ Google Trends với phân trang.
 * Sử dụng PagedResponse<RawTrendItemDTO> để đảm bảo tính generic.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RawTrendsResponse {

    @JsonProperty("total_items")
    private Integer totalItems;

    private String geo;

    private LocalDate date;

    @JsonProperty("data_source")
    private String dataSource; // "serpapi"

    private PagedResponse<RawTrendItemDTO> pagedTrends;
}
