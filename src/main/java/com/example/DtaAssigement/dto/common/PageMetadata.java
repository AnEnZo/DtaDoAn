package com.example.DtaAssigement.dto.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Metadata phân trang - generic, có thể tái sử dụng cho mọi API phân trang.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageMetadata {

    @JsonProperty("page")
    private int page; // 1-based

    @JsonProperty("page_size")
    private int pageSize;

    @JsonProperty("total_items")
    private long totalItems;

    @JsonProperty("total_pages")
    private int totalPages;

    @JsonProperty("has_next")
    private boolean hasNext;

    @JsonProperty("has_previous")
    private boolean hasPrevious;
}
