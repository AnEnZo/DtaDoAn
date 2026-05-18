package com.example.DtaAssigement.dto.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Generic wrapper response cho API phân trang.
 * - content: danh sách items của trang hiện tại
 * - pagination: metadata phân trang
 *
 * @param <T> Kiểu dữ liệu của item trong trang
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PagedResponse<T> {

    private List<T> content;

    private PageMetadata pagination;
}
