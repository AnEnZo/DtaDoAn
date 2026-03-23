package com.example.DtaAssigement.dto.llama;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LlamaHealthResponse {
    private String status;
    private String model;
    private String version;
}
