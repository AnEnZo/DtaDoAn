package com.example.DtaAssigement.dto.serpapi;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SerpRelatedQueriesResponse {
    private List<RelatedQueries> relatedQueries;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RelatedQueries {
        private String query;
        private List<QueryItem> top;
        private List<QueryItem> rising;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QueryItem {
        private String query;
        private Integer value;
        private String link;
        private String extractedValue;
        private String formattedValue;
    }
}
