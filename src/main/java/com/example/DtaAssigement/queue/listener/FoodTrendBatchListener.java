package com.example.DtaAssigement.queue.listener;

import com.example.DtaAssigement.dto.queue.FoodTrendBatchMessage;
import com.example.DtaAssigement.dto.llama.FoodTrendQueryResponse;
import com.example.DtaAssigement.dto.llama.FoodTrendQueryItem;
import com.example.DtaAssigement.dto.llama.FoodTrend;
import com.example.DtaAssigement.service.LlamaFoodAnalysisClient;
import com.example.DtaAssigement.service.TrendingFoodAnalyzerSerpApi;
import com.github.sonus21.rqueue.annotation.RqueueListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Food Trend Batch Listener
 * Processes batches of trending searches asynchronously via Groq API
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class FoodTrendBatchListener {

        private final LlamaFoodAnalysisClient llamaClient;
        private final TrendingFoodAnalyzerSerpApi trendingFoodAnalyzer;

        /**
         * Process food trend batch messages from the queue.
         * Extracts query strings from the batch and sends to Groq API for food
         * analysis.
         */
        @RqueueListener(value = "food-trend-batch-queue", concurrency = "3", numRetries = "2", deadLetterQueue = "food-trend-batch-dlq", visibilityTimeout = "1200000" // 20
                                                                                                                                                                       // minutes
        )
        public void processBatch(FoodTrendBatchMessage message) {
                log.info("🍔 Processing food trend batch {}/{} for geo: {} ({} trends)",
                                message.getBatchNumber(), message.getTotalBatches(),
                                message.getGeo(), message.getTrends().size());

                try {
                        long startTime = System.currentTimeMillis();

                        // Trích query strings từ batch
                        List<String> queries = message.getTrends().stream()
                                        .map(t -> t.getQuery())
                                        .collect(Collectors.toList());

                        // Gửi sang Groq API để phân tích
                        log.debug("Sending batch {}/{} to Groq for food analysis...",
                                        message.getBatchNumber(), message.getTotalBatches());
                        FoodTrendQueryResponse groqResponse = llamaClient.analyzeFoodTrendQueries(queries);

                        if (groqResponse == null || groqResponse.getFoodTrends() == null
                                        || groqResponse.getFoodTrends().isEmpty()) {
                                log.warn("No food trends identified by Groq for batch {}/{}",
                                                message.getBatchNumber(), message.getTotalBatches());
                                return;
                        }

                        // Chuyển FoodTrendQueryItem → FoodTrend để lưu DB
                        List<FoodTrend> foodTrends = groqResponse.getFoodTrends().stream()
                                        .map(item -> FoodTrend.builder()
                                                        .foodName(item.getQuery())
                                                        .reasoning(item.getReasoning())
                                                        .hotnessScore(5) // default score khi không có volume data
                                                        .build())
                                        .collect(Collectors.toList());

                        log.info("Saving {} food trends from batch {}/{} to database...",
                                        foodTrends.size(), message.getBatchNumber(), message.getTotalBatches());
                        trendingFoodAnalyzer.saveTrendsToDatabase(foodTrends, message.getGeo());

                        long duration = System.currentTimeMillis() - startTime;
                        log.info("✅ Batch {}/{} completed in {}ms - {} food trends saved",
                                        message.getBatchNumber(), message.getTotalBatches(),
                                        duration, foodTrends.size());

                } catch (Exception e) {
                        log.error("❌ Failed to process batch {}/{} for geo: {}",
                                        message.getBatchNumber(), message.getTotalBatches(),
                                        message.getGeo(), e);
                        throw e; // Trigger retry
                }
        }

        /**
         * Dead Letter Queue handler for failed batches
         */
        @RqueueListener(value = "food-trend-batch-dlq")
        public void handleFailedBatch(FoodTrendBatchMessage message) {
                log.error("🔥 Food trend batch {}/{} permanently failed. Geo: {}, Trends: {}",
                                message.getBatchNumber(), message.getTotalBatches(),
                                message.getGeo(), message.getTrends().size());
                // TODO: Send alert to admin
        }
}
