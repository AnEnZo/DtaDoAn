package com.example.DtaAssigement.controller;

import com.example.DtaAssigement.dto.serpapi.*;
import com.example.DtaAssigement.dto.foodtrends.FoodTrendsResponse;
import com.example.DtaAssigement.service.SerpAPIClient;
import com.example.DtaAssigement.service.TrendingFoodAnalyzerSerpApi;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * REST Controller for SerpAPI - Google Trends and Baidu
 */
@RestController
@RequestMapping("/api/serpapi")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Slf4j
public class SerpAPIController {

    private final SerpAPIClient serpAPIClient;
    private final TrendingFoodAnalyzerSerpApi trendingFoodAnalyzerSerpApi;

    /**
     * Get Google Trends Trending Now
     */
    @GetMapping("/trending-now")
    public ResponseEntity<SerpTrendingNowResponse> getTrendingNow(
            @RequestParam(defaultValue = "VN") String geo,
            @RequestParam(defaultValue = "24") Integer hours,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(defaultValue = "en") String hl) {
        return ResponseEntity.ok(serpAPIClient.getTrendingNow(geo, hours, categoryId, hl));
    }

    /**
     * Get Google Trends Interest Over Time
     */
    @GetMapping("/interest-over-time")
    public ResponseEntity<SerpInterestOverTimeResponse> getInterestOverTime(
            @RequestParam String q,
            @RequestParam(defaultValue = "VN") String geo,
            @RequestParam(defaultValue = "today 1-m") String date,
            @RequestParam(defaultValue = "en") String hl) {
        return ResponseEntity.ok(serpAPIClient.getInterestOverTime(q, geo, date, hl));
    }

    /**
     * Get Google Trends News
     */
    @GetMapping("/trending-news")
    public ResponseEntity<SerpTrendingNewsResponse> getTrendingNews(
            @RequestParam String newsToken) {
        return ResponseEntity.ok(serpAPIClient.getTrendingNews(newsToken));
    }

    /**
     * Get Google Trends Related Queries
     */
    @GetMapping("/related-queries")
    public ResponseEntity<SerpRelatedQueriesResponse> getRelatedQueries(
            @RequestParam String q,
            @RequestParam(defaultValue = "VN") String geo,
            @RequestParam(defaultValue = "today 1-m") String date,
            @RequestParam(defaultValue = "en") String hl) {
        return ResponseEntity.ok(serpAPIClient.getRelatedQueries(q, geo, date, hl));
    }

    /**
     * Get Google Trends Related Topics
     */
    @GetMapping("/related-topics")
    public ResponseEntity<SerpRelatedTopicsResponse> getRelatedTopics(
            @RequestParam String q,
            @RequestParam(defaultValue = "VN") String geo,
            @RequestParam(defaultValue = "today 1-m") String date,
            @RequestParam(defaultValue = "en") String hl) {
        return ResponseEntity.ok(serpAPIClient.getRelatedTopics(q, geo, date, hl));
    }

    /**
     * Search Baidu (China's search engine)
     */
    @GetMapping("/baidu-search")
    public ResponseEntity<SerpBaiduSearchResponse> searchBaidu(
            @RequestParam String q) {
        return ResponseEntity.ok(serpAPIClient.searchBaidu(q));
    }

    /**
     * Get Google Trends Autocomplete suggestions
     */
    @GetMapping("/autocomplete")
    public ResponseEntity<SerpAutocompleteResponse> getAutocomplete(
            @RequestParam String q,
            @RequestParam(defaultValue = "en") String hl) {
        return ResponseEntity.ok(serpAPIClient.getAutocomplete(q, hl));
    }

    // ===== FOOD TRENDS ENDPOINTS (with Llama AI Analysis) =====

    /**
     * Get current hot food trends
     * Uses SerpAPI + Llama AI to identify food/beverage trends
     * Auto-saves to database with duplicate prevention
     */
    @Operation(summary = "Lấy hot trends món ăn hiện tại", description = "Lấy danh sách món ăn/đồ uống đang hot trend từ Google Trends. "
            +
            "Dữ liệu được phân tích bởi Llama AI và tự động lưu vào database. " +
            "Nếu đã có dữ liệu mới trong 1 giờ qua, sẽ trả về từ database thay vì gọi API mới.")
    @GetMapping("/food-trends/current")
    public ResponseEntity<FoodTrendsResponse> getCurrentHotTrends(
            @Parameter(description = "Mã quốc gia (VN, US, TH, etc.)", example = "VN") @RequestParam(defaultValue = "VN") String geo,

            @Parameter(description = "Số giờ trending (12, 24, 48)", example = "24") @RequestParam(defaultValue = "24") Integer hours,

            @Parameter(description = "Số lượng kết quả tối đa", example = "10") @RequestParam(defaultValue = "10") Integer limit) {
        log.info("API: Get current hot food trends - geo={}, hours={}, limit={}", geo, hours, limit);
        FoodTrendsResponse response = trendingFoodAnalyzerSerpApi.getCurrentHotTrends(geo, hours, limit);
        return ResponseEntity.ok(response);
    }

    /**
     * Get historical food trends from database
     * Only returns data from database, no API calls
     */
    @Operation(summary = "Lấy trends theo ngày từ database", description = "Lấy danh sách món ăn hot trends đã được lưu trong database cho một ngày cụ thể. "
            +
            "Chỉ trả về dữ liệu từ database, không gọi SerpAPI/Llama AI.")
    @GetMapping("/food-trends/history")
    public ResponseEntity<FoodTrendsResponse> getHistoricalTrends(
            @Parameter(description = "Mã quốc gia", example = "VN") @RequestParam(defaultValue = "VN") String geo,

            @Parameter(description = "Ngày cần xem trends (format: yyyy-MM-dd)", example = "2026-02-01") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,

            @Parameter(description = "Số lượng kết quả tối đa", example = "10") @RequestParam(defaultValue = "10") Integer limit) {
        log.info("API: Get historical food trends - geo={}, date={}, limit={}", geo, date, limit);
        FoodTrendsResponse response = trendingFoodAnalyzerSerpApi.getHistoricalTrends(geo, date, limit);
        return ResponseEntity.ok(response);
    }

    /**
     * Get today's food trends (shortcut)
     * Returns today's trends from database
     */
    @Operation(summary = "Lấy trends hôm nay nhanh", description = "Shortcut để lấy trends của hôm nay từ database")
    @GetMapping("/food-trends/today")
    public ResponseEntity<FoodTrendsResponse> getTodayTrends(
            @Parameter(description = "Mã quốc gia", example = "VN") @RequestParam(defaultValue = "VN") String geo,

            @Parameter(description = "Số lượng kết quả tối đa", example = "10") @RequestParam(defaultValue = "10") Integer limit) {
        log.info("API: Get today's food trends - geo={}, limit={}", geo, limit);
        LocalDate today = LocalDate.now();
        FoodTrendsResponse response = trendingFoodAnalyzerSerpApi.getHistoricalTrends(geo, today, limit);
        return ResponseEntity.ok(response);
    }

    // ===== NEW: 2-PHASE FOOD TREND PIPELINE =====

    /**
     * API 1: Fetch từ SerpAPI và lưu vào raw_google_trends.
     * Dedup theo ngày — nếu hôm nay đã fetch rồi thì bỏ qua, không gọi SerpAPI
     * thêm.
     */
    @Operation(summary = "[API 1] Fetch raw trends từ SerpAPI", description = "Bước 1: Fetch dữ liệu thô từ Google Trends qua SerpAPI và lưu vào bảng raw_google_trends. "
            +
            "Nếu hôm nay đã fetch rồi (cùng geo) → trả về status=skipped.")
    @PostMapping("/food-trends/fetch")
    public ResponseEntity<TrendingFoodAnalyzerSerpApi.FetchRawResult> fetchRawTrends(
            @Parameter(description = "Mã quốc gia", example = "VN") @RequestParam(defaultValue = "VN") String geo,
            @Parameter(description = "Số giờ trending", example = "24") @RequestParam(defaultValue = "24") Integer hours) {
        log.info("API: Fetch raw trends - geo={}, hours={}", geo, hours);
        TrendingFoodAnalyzerSerpApi.FetchRawResult result = trendingFoodAnalyzerSerpApi.fetchAndSaveRaw(geo, hours);
        return ResponseEntity.ok(result);
    }

    /**
     * API 2: Phân tích raw trends với Llama, join dữ liệu, lưu vào trending_foods.
     * Per-record dedup — mỗi query chỉ được lưu 1 lần trong ngày.
     * Cần gọi API 1 (fetch) trước.
     */
    @Operation(summary = "[API 2] Analyze raw trends bằng Llama AI", description = "Bước 2: Lấy queries từ raw_google_trends hôm nay → gửi sang Llama → join với raw data → "
            +
            "lưu vào trending_foods. Mỗi query chỉ lưu 1 lần/ngày (per-record dedup). " +
            "Phải gọi /food-trends/fetch trước.")
    @PostMapping("/food-trends/analyze")
    public ResponseEntity<TrendingFoodAnalyzerSerpApi.AnalyzeRawResult> analyzeRawTrends(
            @Parameter(description = "Mã quốc gia", example = "VN") @RequestParam(defaultValue = "VN") String geo) {
        log.info("API: Analyze raw trends with Llama - geo={}", geo);
        TrendingFoodAnalyzerSerpApi.AnalyzeRawResult result = trendingFoodAnalyzerSerpApi.analyzeRawTrends(geo);
        return ResponseEntity.ok(result);
    }

    /**
     * API Pipeline: Gộp fetch (API 1) + analyze (API 2) thành 1 lần gọi.
     * Trả về kết quả tổng hợp của cả 2 bước.
     */
    @Operation(summary = "[API Pipeline] Fetch & Analyze food trends trong 1 bước", description = "Gộp API 1 (fetch SerpAPI → raw_google_trends) và API 2 (Llama → trending_foods) thành 1 lần gọi. "
            + "Nếu hôm nay đã fetch rồi (fetch.status=skipped), bước analyze vẫn chạy bình thường. "
            + "Trả về kết quả tổng hợp của cả 2 bước.")
    @PostMapping("/food-trends/pipeline")
    public ResponseEntity<TrendingFoodAnalyzerSerpApi.PipelineResult> fetchAndAnalyzePipeline(
            @Parameter(description = "Mã quốc gia", example = "VN") @RequestParam(defaultValue = "VN") String geo,
            @Parameter(description = "Số giờ trending", example = "24") @RequestParam(defaultValue = "24") Integer hours) {
        log.info("API Pipeline: fetch+analyze - geo={}, hours={}", geo, hours);
        TrendingFoodAnalyzerSerpApi.PipelineResult result = trendingFoodAnalyzerSerpApi.fetchAndAnalyzePipeline(geo,
                hours);
        return ResponseEntity.ok(result);
    }
}
