package com.example.DtaAssigement.service.impl;

import com.example.DtaAssigement.aop.caching.CacheEvict;
import com.example.DtaAssigement.aop.caching.Cacheable;
import com.example.DtaAssigement.dto.TrendingFoodDTO;
import com.example.DtaAssigement.dto.TrendingNewsArticleDTO;
import com.example.DtaAssigement.dto.searchapi.AutocompleteResponse;
import com.example.DtaAssigement.dto.searchapi.TrendingNewsResponse;
import com.example.DtaAssigement.dto.searchapi.TrendingNowResponse;
import com.example.DtaAssigement.entity.TrendingFood;
import com.example.DtaAssigement.repository.TrendingFoodRepository;
import com.example.DtaAssigement.service.SearchAPIClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service for managing trending foods from Google Trends
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TrendingFoodService {

    private final SearchAPIClient searchAPIClient;
    private final TrendingFoodRepository trendingFoodRepository;

    @Value("${trending.geo:VN}")
    private String geo;

    @Value("${trending.time-window:past_24_hours}")
    private String timeWindow;

    @Value("${trending.min-score:30}")
    private int minScore;

    @Value("${trending.stale-threshold-hours:48}")
    private int staleThresholdHours;

    /**
     * Get top trending foods (cached)
     */
    @Cacheable(key = "trendingFoods", ttl = 3600) // Cache for 1 hour
    public List<TrendingFoodDTO> getTopTrendingFoods() {
        log.debug("Getting top trending foods with min score: {}", minScore);

        List<TrendingFood> trendingFoods = trendingFoodRepository
                .findTopTrendingFoods(minScore);

        return trendingFoods.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get all trending foods by location
     */
    public List<TrendingFoodDTO> getTrendingFoodsByLocation(String location) {
        return trendingFoodRepository.findByLocationOrderByTrendScoreDesc(location)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get news articles for a trending topic
     */
    public List<TrendingNewsArticleDTO> getNewsArticles(String newsToken) {
        log.debug("Fetching news articles for token: {}", newsToken);

        TrendingNewsResponse response = searchAPIClient.getTrendingNews(newsToken);

        if (response == null || response.getArticles() == null) {
            return List.of();
        }

        return response.getArticles().stream()
                .map(article -> TrendingNewsArticleDTO.builder()
                        .title(article.getTitle())
                        .source(article.getSource())
                        .url(article.getUrl())
                        .thumbnailUrl(article.getThumbnailUrl())
                        .timeAgo(article.getTimeAgo())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Get autocomplete suggestions
     */
    public List<String> getAutocompleteSuggestions(String query) {
        if (query == null || query.length() < 3) {
            return List.of();
        }

        AutocompleteResponse response = searchAPIClient.getAutocomplete(query, geo);

        if (response == null || response.getTopics() == null) {
            return List.of();
        }

        return response.getTopics().stream()
                .map(AutocompleteResponse.Topic::getTitle)
                .collect(Collectors.toList());
    }

    // @Scheduled(cron = "${trending.update-cron:0 0 */6 * * *}")
    @Transactional
    @CacheEvict(keys = { "trendingFoods" })
    public void updateTrendingFoods() {
        log.info("Starting scheduled update of trending foods...");

        try {
            // Fetch trending data from SearchAPI
            TrendingNowResponse response = searchAPIClient.getTrendingNow(
                    geo,
                    timeWindow,
                    "food_and_drink");

            if (response == null || response.getTrends() == null) {
                log.warn("No trending data received from SearchAPI");
                return;
            }

            int updated = 0;
            int created = 0;

            // Process each trend
            for (TrendingNowResponse.TrendTopic trend : response.getTrends()) {
                try {
                    boolean isNew = saveTrendingFood(trend);
                    if (isNew) {
                        created++;
                    } else {
                        updated++;
                    }
                } catch (Exception e) {
                    log.error("Error processing trend: {}", trend.getQuery(), e);
                }
            }

            // Archive stale trends
            archiveStaleTrends();

            log.info("Trending foods update completed: {} created, {} updated", created, updated);

        } catch (Exception e) {
            log.error("Error updating trending foods", e);
        }
    }

    /**
     * Manually refresh trending foods (for admin)
     */
    @Transactional
    @CacheEvict(keys = { "trendingFoods" })
    public void manualRefresh() {
        log.info("Manual refresh triggered");
        updateTrendingFoods();
    }

    /**
     * Save or update a trending food
     * 
     * @return true if new record created, false if updated
     */
    private boolean saveTrendingFood(TrendingNowResponse.TrendTopic trend) {
        Optional<TrendingFood> existingOpt = trendingFoodRepository
                .findByQueryAndLocation(trend.getQuery(), trend.getLocation());

        if (existingOpt.isPresent()) {
            // Update existing trend
            TrendingFood existing = existingOpt.get();
            updateExistingTrend(existing, trend);
            trendingFoodRepository.save(existing);
            return false;
        } else {
            // Create new trend
            TrendingFood newTrend = createNewTrend(trend);
            trendingFoodRepository.save(newTrend);
            return true;
        }
    }

    /**
     * Update existing trend with new data
     */
    private void updateExistingTrend(TrendingFood existing, TrendingNowResponse.TrendTopic trend) {
        existing.setSearchVolume(trend.getSearchVolume());
        existing.setPercentageIncrease(trend.getPercentageIncrease());
        existing.setNewsToken(trend.getNewsToken());
        existing.setLastUpdatedAt(LocalDateTime.now());
        existing.setStatus("ACTIVE");

        // Update categories if provided
        if (trend.getCategories() != null && !trend.getCategories().isEmpty()) {
            existing.setCategoriesList(trend.getCategories());
        }

        // Recalculate trend score
        existing.calculateTrendScore();

        log.debug("Updated existing trend: {} (score: {})", existing.getQuery(), existing.getTrendScore());
    }

    /**
     * Create new trending food from API data
     */
    private TrendingFood createNewTrend(TrendingNowResponse.TrendTopic trend) {
        TrendingFood newTrend = TrendingFood.builder()
                .query(trend.getQuery())
                .searchVolume(trend.getSearchVolume())
                .percentageIncrease(trend.getPercentageIncrease())
                .location(trend.getLocation() != null ? trend.getLocation() : geo)
                .newsToken(trend.getNewsToken())
                .status("ACTIVE")
                .firstSeenAt(LocalDateTime.now())
                .lastUpdatedAt(LocalDateTime.now())
                .build();

        // Set categories using helper method
        if (trend.getCategories() != null && !trend.getCategories().isEmpty()) {
            newTrend.setCategoriesList(trend.getCategories());
        }

        // Calculate trend score
        newTrend.calculateTrendScore();

        log.debug("Created new trend: {} (score: {})", newTrend.getQuery(), newTrend.getTrendScore());

        return newTrend;
    }

    /**
     * Archive trends that haven't been updated recently
     */
    private void archiveStaleTrends() {
        LocalDateTime threshold = LocalDateTime.now().minusHours(staleThresholdHours);
        List<TrendingFood> staleTrends = trendingFoodRepository.findStaleActiveTrends(threshold);

        for (TrendingFood trend : staleTrends) {
            trend.setStatus("ARCHIVED");
            trendingFoodRepository.save(trend);
            log.debug("Archived stale trend: {}", trend.getQuery());
        }

        if (!staleTrends.isEmpty()) {
            log.info("Archived {} stale trends", staleTrends.size());
        }
    }

    /**
     * Convert entity to DTO
     */
    private TrendingFoodDTO toDTO(TrendingFood entity) {
        return TrendingFoodDTO.builder()
                .id(entity.getId())
                .query(entity.getQuery())
                .searchVolume(entity.getSearchVolume())
                .percentageIncrease(entity.getPercentageIncrease())
                .trendScore(entity.getTrendScore())
                .categories(entity.getCategoriesList())
                .newsToken(entity.getNewsToken())
                .location(entity.getLocation())
                .firstSeenAt(entity.getFirstSeenAt())
                .lastUpdatedAt(entity.getLastUpdatedAt())
                .status(entity.getStatus())
                .menuItemId(entity.getMenuItem() != null ? entity.getMenuItem().getId() : null)
                .menuItemName(entity.getMenuItem() != null ? entity.getMenuItem().getName() : null)
                .build();
    }

    // --- Raw API Pass-through Methods ---

    public Object getRawTrendingFoods(String geo, String time) {
        return searchAPIClient.getRawTrendingNow(geo, time, "food_and_drink");
    }

    public Object getRawTrendingNews(String newsToken) {
        return searchAPIClient.getRawTrendingNews(newsToken);
    }

    public Object getRawAutocomplete(String query) {
        return searchAPIClient.getRawAutocomplete(query, geo);
    }

    public Object getRawGoogleRank(String query, String location) {
        // Default to configured geo if location is null
        String loc = (location != null && !location.isEmpty()) ? location : geo;
        return searchAPIClient.getRawGoogleRank(query, loc);
    }
}
