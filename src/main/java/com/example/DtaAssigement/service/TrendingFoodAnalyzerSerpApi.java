package com.example.DtaAssigement.service;

import com.example.DtaAssigement.dto.common.PageMetadata;
import com.example.DtaAssigement.dto.common.PagedResponse;
import com.example.DtaAssigement.dto.foodtrends.FoodTrendItemDTO;
import com.example.DtaAssigement.dto.foodtrends.FoodTrendsResponse;
import com.example.DtaAssigement.dto.foodtrends.RawTrendItemDTO;
import com.example.DtaAssigement.dto.foodtrends.RawTrendsResponse;
import com.example.DtaAssigement.dto.llama.FoodTrend;
import com.example.DtaAssigement.dto.llama.FoodTrendQueryItem;
import com.example.DtaAssigement.dto.llama.FoodTrendQueryResponse;
import com.example.DtaAssigement.dto.queue.FoodTrendBatchMessage;
import com.example.DtaAssigement.dto.serpapi.SerpTrendingNowResponse.Category;
import com.example.DtaAssigement.dto.serpapi.SerpTrendingNowResponse;
import com.example.DtaAssigement.entity.RawGoogleTrend;
import com.example.DtaAssigement.entity.TrendingFood;
import com.example.DtaAssigement.repository.RawGoogleTrendRepository;
import com.example.DtaAssigement.repository.TrendingFoodRepository;
import com.github.sonus21.rqueue.core.RqueueMessageEnqueuer;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

/**
 * Service for analyzing food trends using SerpAPI and Llama AI
 * with database persistence for historical tracking
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TrendingFoodAnalyzerSerpApi {

    private final SerpAPIClient serpAPIClient;
    private final LlamaFoodAnalysisClient llamaClient;
    private final TrendingFoodRepository trendingFoodRepository;
    private final RawGoogleTrendRepository rawGoogleTrendRepository;
    private final RqueueMessageEnqueuer rqueueMessageEnqueuer;

    /**
     * Get current hot food trends
     * Flow: Check DB -> If stale, fetch from SerpAPI + Llama -> Save to DB ->
     * Return
     *
     * @param geo   Location code (e.g., "VN")
     * @param hours Time range in hours (e.g., 24)
     * @param limit Maximum number of results
     * @return Food trends response
     */
    public FoodTrendsResponse getCurrentHotTrends(String geo, Integer hours, Integer limit) {
        log.info("Getting current hot trends: geo={}, hours={}, limit={}", geo, hours, limit);

        // Step 1: Check if we have fresh data in database (updated today within last
        // hour)
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        List<TrendingFood> recentTrends = trendingFoodRepository
                .findByLocationAndLastUpdatedAtAfterOrderByTrendScoreDesc(geo, oneHourAgo);

        if (!recentTrends.isEmpty()) {
            log.info("Found {} fresh trends in database, returning from cache", recentTrends.size());
            return buildResponseFromDatabase(recentTrends, geo, hours + " hours", "database", false, null);
        }

        // Step 2: Fetch fresh data from SerpAPI
        log.info("No fresh data in DB, fetching from SerpAPI...");
        SerpTrendingNowResponse serpResponse = serpAPIClient.getTrendingNow(geo, hours, null, "vi");

        if (serpResponse == null || serpResponse.getTrendingSearches() == null) {
            log.warn("No trending data from SerpAPI");
            return FoodTrendsResponse.builder()
                    .totalTrends(0)
                    .geo(geo)
                    .timeRange(hours + " hours")
                    .dataSource("serpapi")
                    .savedToDatabase(false)
                    .trends(List.of())
                    .build();
        }

        // Step 3: Split into batches and enqueue for async processing
        List<SerpTrendingNowResponse.TrendingSearch> allTrends = serpResponse.getTrendingSearches();
        int batchSize = 3;
        int totalBatches = (int) Math.ceil((double) allTrends.size() / batchSize);

        log.info("Splitting {} trends into {} batches of up to {} trends each",
                allTrends.size(), totalBatches, batchSize);

        for (int i = 0; i < totalBatches; i++) {
            int start = i * batchSize;
            int end = Math.min(start + batchSize, allTrends.size());
            List<SerpTrendingNowResponse.TrendingSearch> batch = allTrends.subList(start, end);

            FoodTrendBatchMessage message = FoodTrendBatchMessage.builder()
                    .geo(geo)
                    .batchNumber(i + 1)
                    .totalBatches(totalBatches)
                    .trends(batch)
                    .build();

            rqueueMessageEnqueuer.enqueue("food-trend-batch-queue", message);
            log.debug("Enqueued batch {}/{} with {} trends", i + 1, totalBatches, batch.size());
        }

        log.info("Successfully enqueued {} batches for async processing", totalBatches);

        // Step 4: Return current data from database (may be empty if first time)
        // The queue will process batches asynchronously and populate the database
        List<TrendingFood> currentTrends = trendingFoodRepository
                .findByLocationAndLastUpdatedAtAfterOrderByTrendScoreDesc(geo, LocalDateTime.now().minusDays(1));

        return buildResponseFromDatabase(
                currentTrends,
                geo,
                hours + " hours",
                "serpapi_batch_processing",
                false,
                null);
    }

    // =========================================================================
    // API 1: Fetch raw data from SerpAPI, save to raw_google_trends
    // =========================================================================

    /**
     * API 1: Fetch trending searches from SerpAPI và lưu vào raw_google_trends.
     * Dedup check (table-level): nếu đã có data cho (geo, hôm nay) thì skip.
     */
    @Transactional
    public FetchRawResult fetchAndSaveRaw(String geo, Integer hours) {
        LocalDate today = LocalDate.now();

        // Dedup check: đã có data hôm nay chưa?
        if (rawGoogleTrendRepository.existsByLocationAndFetchedDate(geo, today)) {
            log.info("⏭️ Raw trends already exist for geo={} today={}, skipping fetch", geo, today);
            return FetchRawResult.skipped(geo);
        }

        // Fetch từ SerpAPI
        log.info("📡 Fetching trends from SerpAPI: geo={}, hours={}", geo, hours);
        SerpTrendingNowResponse serpResponse = serpAPIClient.getTrendingNow(geo, hours, null, "vi");

        if (serpResponse == null || serpResponse.getTrendingSearches() == null
                || serpResponse.getTrendingSearches().isEmpty()) {
            log.warn("⚠️ No trending data returned from SerpAPI for geo={}", geo);
            return FetchRawResult.empty(geo);
        }

        // Lưu từng item vào raw_google_trends (loại bỏ trùng lặp trong memory)
        List<SerpTrendingNowResponse.TrendingSearch> allTrends = serpResponse.getTrendingSearches();
        Map<String, RawGoogleTrend> uniqueTrendsMap = new java.util.LinkedHashMap<>();

        for (SerpTrendingNowResponse.TrendingSearch trend : allTrends) {
            String query = trend.getQuery();
            if (query == null || query.isBlank()) {
                continue;
            }
            String normalizedQuery = query.trim();

            if (uniqueTrendsMap.containsKey(normalizedQuery)) {
                log.info("⏭️ Skipping duplicate trend query in API response: '{}'", normalizedQuery);
                continue;
            }

            RawGoogleTrend raw = RawGoogleTrend.builder()
                    .query(normalizedQuery)
                    .searchVolume(trend.getSearchVolume() != null ? trend.getSearchVolume().longValue() : null)
                    .increasePercentage(trend.getIncreasePercentage())
                    .location(geo)
                    .fetchedDate(today)
                    .fetchedAt(LocalDateTime.now())
                    .build();

            // Set categories (names only, not full objects)
            if (trend.getCategories() != null) {
                List<String> categoryNames = trend.getCategories().stream()
                        .map(Category::getName)
                        .limit(10)
                        .collect(Collectors.toList());
                raw.setCategoriesList(categoryNames);
            }

            // Set trend breakdown (related queries) nếu có
            if (trend.getTrendBreakdown() != null) {
                raw.setTrendBreakdownList(trend.getTrendBreakdown().stream()
                        .limit(10)
                        .collect(Collectors.toList()));
            }

            uniqueTrendsMap.put(normalizedQuery, raw);
        }

        List<RawGoogleTrend> toSave = new ArrayList<>(uniqueTrendsMap.values());
        rawGoogleTrendRepository.saveAll(toSave);
        log.info("✅ Saved {} raw trends to DB (after in-memory deduplication) for geo={}, date={}", toSave.size(), geo, today);
        return FetchRawResult.ok(geo, toSave.size());
    }

    // =========================================================================
    // API 2: Analyze raw trends with Llama, join raw table, save to trending_foods
    // =========================================================================

    /**
     * API 2: Lấy queries từ raw_google_trends → gửi sang Llama → join raw table
     * → lưu vào trending_foods với per-record dedup check.
     */
    @Transactional
    public AnalyzeRawResult analyzeRawTrends(String geo) {
        LocalDate today = LocalDate.now();

        // Lấy tất cả raw trends hôm nay
        List<RawGoogleTrend> rawTrends = rawGoogleTrendRepository
                .findByLocationAndFetchedDate(geo, today);

        if (rawTrends.isEmpty()) {
            log.warn("⚠️ No raw trends found for geo={} date={}, run fetchAndSaveRaw first", geo, today);
            return AnalyzeRawResult.empty(geo);
        }

        // Trích queries → gửi sang Llama
        List<String> queries = rawTrends.stream()
                .map(RawGoogleTrend::getQuery)
                .collect(Collectors.toList());

        log.info("🍜 Sending {} queries to Llama analyze-food-trends", queries.size());
        FoodTrendQueryResponse llamaResponse = llamaClient.analyzeFoodTrendQueries(queries);

        if (llamaResponse.getFoodTrends() == null || llamaResponse.getFoodTrends().isEmpty()) {
            log.info("🍜 Llama found no food trends among {} queries", queries.size());
            return AnalyzeRawResult.of(geo, queries.size(), 0, 0, 0);
        }

        // Build raw lookup map: query → RawGoogleTrend
        Map<String, RawGoogleTrend> rawMap = rawTrends.stream()
                .collect(Collectors.toMap(RawGoogleTrend::getQuery, Function.identity(), (a, b) -> a));

        int saved = 0;
        int skipped = 0;

        for (FoodTrendQueryItem item : llamaResponse.getFoodTrends()) {
            String query = item.getQuery();

            // Per-record dedup: đã lưu hôm nay chưa?
            if (trendingFoodRepository.existsByQueryAndLocationAndDate(query, geo, today)) {
                log.debug("⏭️ Skipping already-saved trend: query='{}' geo='{}'", query, geo);
                skipped++;
                continue;
            }

            // JOIN: lấy volume/score từ raw table
            RawGoogleTrend raw = rawMap.get(query);
            if (raw == null) {
                log.warn("⚠️ Llama returned query '{}' not found in raw table, skipping", query);
                skipped++;
                continue;
            }

            // Tạo và lưu TrendingFood mới
            TrendingFood food = TrendingFood.builder()
                    .query(query)
                    .searchVolume(raw.getSearchVolume())
                    .percentageIncrease(raw.getIncreasePercentage())
                    .location(geo)
                    .status("ACTIVE")
                    .dataSource("GOOGLE_TRENDS")
                    .firstSeenAt(LocalDateTime.now())
                    .lastUpdatedAt(LocalDateTime.now())
                    .build();

            food.calculateTrendScore();
            trendingFoodRepository.save(food);
            saved++;

            log.debug("✅ Saved food trend: query='{}', score={}, volume={}",
                    query, food.getTrendScore(), raw.getSearchVolume());
        }

        log.info("🍜 analyzeRawTrends complete: analyzed={}, food_found={}, saved={}, skipped={}",
                queries.size(), llamaResponse.getTotalFoodTrends(), saved, skipped);

        return AnalyzeRawResult.of(geo, queries.size(), llamaResponse.getTotalFoodTrends(), saved, skipped);
    }

    // =========================================================================
    // API Pipeline: fetch + analyze in one call
    // =========================================================================

    /**
     * Pipeline gộp API 1 (fetch) + API 2 (analyze) trong một lần gọi.
     * Nếu hôm nay đã fetch rồi (status=skipped), vẫn tiếp tục analyze.
     *
     * @param geo   Mã quốc gia (VN, US, ...)
     * @param hours Số giờ trending
     * @return PipelineResult chứa kết quả của cả 2 bước
     */
    @Transactional
    public PipelineResult fetchAndAnalyzePipeline(String geo, Integer hours) {
        log.info("🚀 Pipeline start: geo={}, hours={}", geo, hours);

        // Bước 1: Fetch raw từ SerpAPI → raw_google_trends
        FetchRawResult fetchResult = fetchAndSaveRaw(geo, hours);
        log.info("✅ Pipeline step 1 done: status={}, fetched={}", fetchResult.getStatus(), fetchResult.getFetched());

        // Bước 2: Analyze raw → Llama → trending_foods
        // Luôn chạy dù fetch là "ok" hay "skipped" để không bỏ sót data
        AnalyzeRawResult analyzeResult = analyzeRawTrends(geo);
        log.info("✅ Pipeline step 2 done: analyzed={}, saved={}, skipped={}",
                analyzeResult.getAnalyzed(), analyzeResult.getSaved(), analyzeResult.getSkipped());

        return new PipelineResult(fetchResult, analyzeResult);
    }

    // =========================================================================
    // API: Get raw trends from database
    // =========================================================================

    /**
     * Get raw trends from database for display in frontend — phân trang offset.
     * Nếu date = null thì get all, không lọc theo ngày.
     *
     * @param geo   Mã quốc gia (VN, US, ...), null = get all locations
     * @param date  Ngày cần xem raw trends, null = get all dates
     * @param page  Trang (1-based)
     * @param limit Kích thước trang
     * @return RawTrendsResponse chứa danh sách raw trends + pagination metadata
     */
    public RawTrendsResponse getRawTrends(String geo, LocalDate date, int page, int limit) {
        log.info("Getting raw trends (paginated): geo={}, date={}, page={}, limit={}", geo, date, page, limit);

        int pageZeroBased = Math.max(0, page - 1);
        Pageable pageable = PageRequest.of(pageZeroBased, limit);

        Page<RawGoogleTrend> trendPage;
        if (date != null && geo != null) {
            trendPage = rawGoogleTrendRepository.findByLocationAndFetchedDateOrderByFetchedAtDesc(geo, date, pageable);
        } else if (geo != null) {
            trendPage = rawGoogleTrendRepository.findByLocationOrderByFetchedAtDesc(geo, pageable);
        } else {
            trendPage = rawGoogleTrendRepository.findAllByOrderByFetchedAtDesc(pageable);
        }

        // Build pagination metadata
        PageMetadata pagination = PageMetadata.builder()
                .page(trendPage.getNumber() + 1) // back to 1-based
                .pageSize(trendPage.getSize())
                .totalItems(trendPage.getTotalElements())
                .totalPages(trendPage.getTotalPages())
                .hasNext(trendPage.hasNext())
                .hasPrevious(trendPage.hasPrevious())
                .build();

        // Map entities to DTOs
        List<RawTrendItemDTO> items = trendPage.getContent().stream()
                .map(this::mapRawEntityToDTO)
                .collect(Collectors.toList());

        // Build generic paged response
        PagedResponse<RawTrendItemDTO> pagedTrends = PagedResponse.<RawTrendItemDTO>builder()
                .content(items)
                .pagination(pagination)
                .build();

        return RawTrendsResponse.builder()
                .totalItems((int) trendPage.getTotalElements())
                .geo(geo)
                .date(date)
                .dataSource("serpapi")
                .pagedTrends(pagedTrends)
                .build();
    }

    /**
     * Map RawGoogleTrend entity to DTO
     */
    private RawTrendItemDTO mapRawEntityToDTO(RawGoogleTrend entity) {
        return RawTrendItemDTO.builder()
                .id(entity.getId())
                .query(entity.getQuery())
                .searchVolume(entity.getSearchVolume())
                .increasePercentage(entity.getIncreasePercentage())
                .categories(entity.getCategories())
                .trendBreakdown(entity.getTrendBreakdown())
                .location(entity.getLocation())
                .fetchedDate(entity.getFetchedDate())
                .fetchedAt(entity.getFetchedAt())
                .build();
    }

    // =========================================================================
    // Inner result classes
    // =========================================================================

    @Data
    public static class FetchRawResult {
        private final String status; // ok | skipped | empty
        private final String geo;
        private final int fetched;
        private final String reason;

        public static FetchRawResult ok(String geo, int count) {
            return new FetchRawResult("ok", geo, count, null);
        }

        public static FetchRawResult skipped(String geo) {
            return new FetchRawResult("skipped", geo, 0, "data already exists for today");
        }

        public static FetchRawResult empty(String geo) {
            return new FetchRawResult("empty", geo, 0, "no data returned from SerpAPI");
        }
    }

    @Data
    public static class AnalyzeRawResult {
        private final String geo;
        private final int analyzed;
        private final int foodTrendsFound;
        private final int saved;
        private final int skipped;

        public static AnalyzeRawResult empty(String geo) {
            return new AnalyzeRawResult(geo, 0, 0, 0, 0);
        }

        public static AnalyzeRawResult of(String geo, int analyzed, int found, int saved, int skipped) {
            return new AnalyzeRawResult(geo, analyzed, found, saved, skipped);
        }
    }

    @Data
    public static class PipelineResult {
        private final FetchRawResult fetch;
        private final AnalyzeRawResult analyze;
    }

    /**
     * Get all food trends from database with offset pagination.
     * No date filter — returns everything matching the geo (or all if geo is null).
     *
     * @param geo   Location code (VN, US, ...), null = all locations
     * @param page  1-based page number
     * @param limit Items per page
     * @return PagedResponse containing paginated food trends + metadata
     */
    public PagedResponse<FoodTrendItemDTO> getAllTrendsPaginated(String geo, int page, int limit) {
        log.info("Getting all trends (paginated): geo={}, page={}, limit={}", geo, page, limit);

        int pageZeroBased = Math.max(0, page - 1);
        Pageable pageable = PageRequest.of(pageZeroBased, limit);

        Page<TrendingFood> trendPage;
        if (geo != null && !geo.isBlank()) {
            trendPage = trendingFoodRepository.findByLocationOrderByLastUpdatedAtDesc(geo, pageable);
        } else {
            trendPage = trendingFoodRepository.findAllByOrderByLastUpdatedAtDesc(pageable);
        }

        PageMetadata pagination = PageMetadata.builder()
                .page(trendPage.getNumber() + 1)
                .pageSize(trendPage.getSize())
                .totalItems(trendPage.getTotalElements())
                .totalPages(trendPage.getTotalPages())
                .hasNext(trendPage.hasNext())
                .hasPrevious(trendPage.hasPrevious())
                .build();

        List<FoodTrendItemDTO> items = trendPage.getContent().stream()
                .map(this::mapEntityToDTO)
                .collect(Collectors.toList());

        return PagedResponse.<FoodTrendItemDTO>builder()
                .content(items)
                .pagination(pagination)
                .build();
    }

    /**
     * Get today's food trends from database (shortcut).
     * Used by the /food-trends/today endpoint.
     */
    public FoodTrendsResponse getTodayTrends(String geo, Integer limit) {
        LocalDate today = LocalDate.now();
        log.info("Getting today's food trends: geo={}, date={}, limit={}", geo, today, limit);

        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.plusDays(1).atStartOfDay();

        List<TrendingFood> trends = trendingFoodRepository
                .findByLocationAndLastUpdatedAtBetweenOrderByTrendScoreDesc(geo, startOfDay, endOfDay);

        if (limit != null && trends.size() > limit) {
            trends = trends.subList(0, limit);
        }

        return buildResponseFromDatabase(trends, geo, null, "database", false, today);
    }

    /**
     * Save food trends to database with duplicate prevention
     * Rule: Each food (query) can only be saved ONCE per day per location
     *
     * @param trends List of food trends from Llama AI
     * @param geo    Location code
     */
    @Transactional
    public void saveTrendsToDatabase(List<FoodTrend> trends, String geo) {
        LocalDate today = LocalDate.now();

        for (FoodTrend trend : trends) {
            try {
                // Check if exists for this location
                Optional<TrendingFood> existingOpt = trendingFoodRepository
                        .findByQueryAndLocation(trend.getFoodName(), geo);

                if (existingOpt.isPresent()) {
                    TrendingFood existing = existingOpt.get();

                    // Check if already updated today
                    if (existing.getLastUpdatedAt().toLocalDate().equals(today)) {
                        log.debug("Skipping '{}': already saved today", trend.getFoodName());
                        continue; // SKIP - already saved today
                    }

                    // Update existing record
                    existing.setSearchVolume(
                            trend.getSearchVolume() != null ? trend.getSearchVolume().longValue() : 0L);
                    existing.setPercentageIncrease(trend.getIncreasePercentage());
                    existing.setTrendScore(trend.getHotnessScore());
                    existing.setLastUpdatedAt(LocalDateTime.now());
                    existing.setStatus("ACTIVE");
                    existing.setDataSource("LLAMA_AI");

                    // Set categories from related keywords
                    if (trend.getRelatedKeywords() != null && !trend.getRelatedKeywords().isEmpty()) {
                        existing.setCategoriesList(trend.getRelatedKeywords());
                    }

                    trendingFoodRepository.save(existing);
                    log.info("Updated trend: '{}' with score={}", trend.getFoodName(), trend.getHotnessScore());

                } else {
                    // Create new record
                    TrendingFood newFood = TrendingFood.builder()
                            .query(trend.getFoodName())
                            .searchVolume(trend.getSearchVolume() != null ? trend.getSearchVolume().longValue() : 0L)
                            .percentageIncrease(trend.getIncreasePercentage())
                            .trendScore(trend.getHotnessScore())
                            .location(geo)
                            .dataSource("LLAMA_AI")
                            .status("ACTIVE")
                            .firstSeenAt(LocalDateTime.now())
                            .lastUpdatedAt(LocalDateTime.now())
                            .build();

                    // Set categories from related keywords
                    if (trend.getRelatedKeywords() != null && !trend.getRelatedKeywords().isEmpty()) {
                        newFood.setCategoriesList(trend.getRelatedKeywords());
                    }

                    trendingFoodRepository.save(newFood);
                    log.info("Created new trend: '{}'", trend.getFoodName());
                }

            } catch (Exception e) {
                log.error("Error saving trend '{}': {}", trend.getFoodName(), e.getMessage(), e);
                // Continue with next trend instead of failing entire batch
            }
        }

        log.info("Successfully processed {} trends for database", trends.size());
    }

    /**
     * Build response from database entities
     */
    private FoodTrendsResponse buildResponseFromDatabase(
            List<TrendingFood> trends,
            String geo,
            String timeRange,
            String dataSource,
            Boolean savedToDatabase,
            LocalDate date) {

        List<FoodTrendItemDTO> trendItems = trends.stream()
                .map(this::mapEntityToDTO)
                .collect(Collectors.toList());

        return FoodTrendsResponse.builder()
                .totalTrends(trends.size())
                .geo(geo)
                .timeRange(timeRange)
                .dataSource(dataSource)
                .savedToDatabase(savedToDatabase)
                .date(date)
                .trends(trendItems)
                .build();
    }

    /**
     * Map Llama AI FoodTrend to DTO
     */
    private FoodTrendItemDTO mapToDTO(FoodTrend trend) {
        FoodTrendItemDTO dto = new FoodTrendItemDTO();
        dto.setFoodName(trend.getFoodName());
        dto.setFoodType(trend.getFoodType());
        dto.setHotnessScore(trend.getHotnessScore());
        dto.setSearchVolume(trend.getSearchVolume() != null ? trend.getSearchVolume().longValue() : 0L);
        dto.setIncreasePercentage(trend.getIncreasePercentage());
        dto.setRelatedKeywords(trend.getRelatedKeywords());
        dto.setReasoning(trend.getReasoning());
        dto.setStatus("ACTIVE");
        dto.setFirstSeenAt(LocalDateTime.now());
        dto.setLastUpdatedAt(LocalDateTime.now());
        return dto;
    }

    /**
     * Map TrendingFood entity to DTO
     */
    private FoodTrendItemDTO mapEntityToDTO(TrendingFood entity) {
        FoodTrendItemDTO dto = new FoodTrendItemDTO();
        dto.setId(entity.getId());
        dto.setFoodName(entity.getQuery());
        dto.setFoodType(null); // Not stored in entity currently
        dto.setHotnessScore(entity.getTrendScore());
        dto.setSearchVolume(entity.getSearchVolume());
        dto.setIncreasePercentage(entity.getPercentageIncrease());
        dto.setRelatedKeywords(entity.getCategoriesList());
        dto.setReasoning(null); // Not stored in entity
        dto.setStatus(entity.getStatus());
        dto.setFirstSeenAt(entity.getFirstSeenAt());
        dto.setLastUpdatedAt(entity.getLastUpdatedAt());
        return dto;
    }
}
