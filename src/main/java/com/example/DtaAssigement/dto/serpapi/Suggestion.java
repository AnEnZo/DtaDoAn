package com.example.DtaAssigement.dto.serpapi;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Suggestion {
    private String q;
    private String title;
    private String type;
    private String link;

    @JsonProperty("serpapi_link")
    private String serpapiLink;
}
