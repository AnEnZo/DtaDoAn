package com.example.DtaAssigement.dto.llama;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FoodTrendQueryRequest {
    private List<String> queries;
}
