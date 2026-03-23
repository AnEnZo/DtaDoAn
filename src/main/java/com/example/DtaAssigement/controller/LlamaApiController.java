package com.example.DtaAssigement.controller;

import com.example.DtaAssigement.dto.llama.*;
import com.example.DtaAssigement.service.LlamaFoodAnalysisClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/**
 * Controller for Groq-powered Food Analysis endpoints
 */
@RestController
@RequestMapping("/api/llama")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Groq Food Analysis API", description = "Phân tích xu hướng món ăn từ Google Trends bằng Groq AI")
public class LlamaApiController {

    private final LlamaFoodAnalysisClient llamaClient;

    @Operation(summary = "Phân tích danh sách query", description = "Gửi danh sách từ khóa trending, trả về những query liên quan F&B")
    @PostMapping("/analyze-queries")
    public ResponseEntity<FoodTrendQueryResponse> analyzeQueries(
            @RequestBody FoodTrendQueryRequest request) {

        if (request.getQueries() == null || request.getQueries().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "queries cannot be empty");
        }

        log.info("Analyzing {} queries via Groq", request.getQueries().size());
        FoodTrendQueryResponse response = llamaClient.analyzeFoodTrendQueries(request.getQueries());
        return ResponseEntity.ok(response);
    }
}
