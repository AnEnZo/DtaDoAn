package com.example.DtaAssigement.queue.listener;

import com.example.DtaAssigement.dto.queue.TrendingFoodUpdateMessage;
import com.example.DtaAssigement.service.impl.TrendingFoodService;
import com.github.sonus21.rqueue.annotation.RqueueListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Trending Food Update Listener
 * Processes trending food update tasks asynchronously
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class TrendingFoodUpdateListener {

    private final TrendingFoodService trendingFoodService;

    /**
     * Process trending food update messages from the queue
     * High priority, retry 2 times on failure
     */
    @RqueueListener(value = "trending-food-update-queue", priority = "1", numRetries = "2", deadLetterQueue = "trending-food-dlq")
    public void processTrendingUpdate(TrendingFoodUpdateMessage message) {
        log.info("🔥 Processing trending food update for geo: {}", message.getGeo());

        try {
            long startTime = System.currentTimeMillis();

            // Perform the update
            trendingFoodService.updateTrendingFoods();

            long duration = System.currentTimeMillis() - startTime;
            log.info("✅ Trending food update completed in {}ms", duration);

        } catch (Exception e) {
            log.error("❌ Trending food update failed", e);
            throw e; // Trigger retry
        }
    }

    /**
     * Dead Letter Queue handler for failed updates
     */
    @RqueueListener(value = "trending-food-dlq")
    public void handleFailedUpdate(TrendingFoodUpdateMessage message) {
        log.error("🔥 Trending food update permanently failed. Geo: {}, TimeWindow: {}",
                message.getGeo(), message.getTimeWindow());
        // TODO: Send alert to admin
    }
}
