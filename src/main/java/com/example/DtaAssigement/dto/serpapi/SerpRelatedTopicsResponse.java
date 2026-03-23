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
public class SerpRelatedTopicsResponse {
    private List<RelatedTopics> relatedTopics;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RelatedTopics {
        private String query;
        private List<TopicItem> top;
        private List<TopicItem> rising;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopicItem {
        private TopicInfo topic;
        private Integer value;
        private String link;
        private String extractedValue;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopicInfo {
        private String mid;
        private String title;
        private String type;
    }
}
