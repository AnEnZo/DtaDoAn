package com.example.DtaAssigement.dto.searchapi;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * Response model for Google Trends Autocomplete API
 */
@Data
public class AutocompleteResponse {

    @JsonProperty("search_metadata")
    private SearchMetadata searchMetadata;

    @JsonProperty("topics")
    private List<Topic> topics;

    @Data
    public static class SearchMetadata {
        private String id;
        private String status;
    }

    @Data
    public static class Topic {
        private String title;
        private String type;

        @JsonProperty("search_id")
        private String searchId;
    }
}
