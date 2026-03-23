package com.example.DtaAssigement.dto.serpapi;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for Baidu Search API
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SerpBaiduSearchResponse {

    @JsonProperty("organic_results")
    private List<OrganicResult> organicResults;

    @JsonProperty("pagination")
    private Pagination pagination;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrganicResult {
        private Integer position;
        private String title;
        private String link;
        private String displayed​Link;
        private String snippet;

        @JsonProperty("cached_page_link")
        private String cachedPageLink;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Pagination {
        private Integer current;
        private String next;

        @JsonProperty("other_pages")
        private List<OtherPage> otherPages;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OtherPage {
        private Integer page;
        private String link;
    }
}
