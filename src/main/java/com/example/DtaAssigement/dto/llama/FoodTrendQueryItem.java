package com.example.DtaAssigement.dto.llama;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FoodTrendQueryItem {
    private String query;
    private String reasoning;
}
