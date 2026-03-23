package com.example.DtaAssigement.dto.llama;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LlamaGenerateRequest {
    private String prompt;
}
