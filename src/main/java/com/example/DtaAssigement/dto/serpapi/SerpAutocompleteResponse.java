package com.example.DtaAssigement.dto.serpapi;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for Google Trends Autocomplete API
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SerpAutocompleteResponse {
    @JsonProperty("search_metadata")
    private SearchMetadata searchMetadata;

    @JsonProperty("search_parameters")
    private SearchParameters searchParameters;

    private List<Suggestion> suggestions;
}
