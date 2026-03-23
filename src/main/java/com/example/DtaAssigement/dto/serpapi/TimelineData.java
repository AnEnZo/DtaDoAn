package com.example.DtaAssigement.dto.serpapi;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimelineData {
    private String date;
    private String timestamp;
    private List<Integer> values;
}
