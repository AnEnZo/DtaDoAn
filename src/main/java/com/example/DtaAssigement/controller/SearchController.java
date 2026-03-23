package com.example.DtaAssigement.controller;

import com.example.DtaAssigement.aop.authorization.RequiresRole;
import com.example.DtaAssigement.dto.TrendingFoodDTO;
import com.example.DtaAssigement.dto.TrendingNewsArticleDTO;
import com.example.DtaAssigement.service.impl.TrendingFoodService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for Trending Foods API
 */
@RestController
@RequestMapping("/api/trending")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class SearchController {

    private final TrendingFoodService trendingFoodService;

    /**
     * Get top trending foods
     * Cached for 1 hour
     */
    @GetMapping("/foods")
    public ResponseEntity<List<TrendingFoodDTO>> getTrendingFoods() {
        List<TrendingFoodDTO> trendingFoods = trendingFoodService.getTopTrendingFoods();
        return ResponseEntity.ok(trendingFoods);
    }

    /**
     * Get trending foods by location
     */
    @GetMapping("/foods/location/{location}")
    public ResponseEntity<List<TrendingFoodDTO>> getTrendingFoodsByLocation(
            @PathVariable String location) {
        List<TrendingFoodDTO> trendingFoods = trendingFoodService
                .getTrendingFoodsByLocation(location.toUpperCase());
        return ResponseEntity.ok(trendingFoods);
    }

    /**
     * Get news articles for a trending topic
     */
    @GetMapping("/news/{newsToken}")
    public ResponseEntity<List<TrendingNewsArticleDTO>> getTrendingNews(
            @PathVariable String newsToken) {
        List<TrendingNewsArticleDTO> articles = trendingFoodService
                .getNewsArticles(newsToken);
        return ResponseEntity.ok(articles);
    }

    /**
     * Get autocomplete suggestions
     * Minimum 3 characters required
     */
    @GetMapping("/autocomplete")
    public ResponseEntity<List<String>> getAutocomplete(
            @RequestParam String q) {
        if (q == null || q.length() < 3) {
            return ResponseEntity.badRequest().build();
        }

        List<String> suggestions = trendingFoodService.getAutocompleteSuggestions(q);
        return ResponseEntity.ok(suggestions);
    }

    /**
     * Manually refresh trending foods (Admin only)
     */
    @PostMapping("/refresh")
    @RequiresRole("ADMIN")
    public ResponseEntity<String> refreshTrends() {
        trendingFoodService.manualRefresh();
        return ResponseEntity.ok("Trending foods refreshed successfully");
    }

    // --- Raw APIs (Direct from Google Trends) ---

    @GetMapping("/raw/now")
    public ResponseEntity<Object> getRawTrendingNow(
            @RequestParam(defaultValue = "VN") String geo,
            @RequestParam(defaultValue = "past_24_hours") String time) {
        return ResponseEntity.ok(trendingFoodService.getRawTrendingFoods(geo, time));
    }

    @GetMapping("/raw/news/{newsToken}")
    public ResponseEntity<Object> getRawTrendingNews(@PathVariable String newsToken) {
        return ResponseEntity.ok(trendingFoodService.getRawTrendingNews(newsToken));
    }

    @GetMapping("/raw/autocomplete")
    public ResponseEntity<Object> getRawAutocomplete(@RequestParam String q) {
        return ResponseEntity.ok(trendingFoodService.getRawAutocomplete(q));
    }

    @GetMapping("/raw/rank")
    public ResponseEntity<Object> getRawGoogleRank(
            @RequestParam String q,
            @RequestParam(required = false) String location) {
        return ResponseEntity.ok(trendingFoodService.getRawGoogleRank(q, location));
    }
}
