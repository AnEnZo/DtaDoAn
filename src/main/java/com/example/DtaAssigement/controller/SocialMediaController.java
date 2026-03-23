package com.example.DtaAssigement.controller;

import com.example.DtaAssigement.dto.*;
import com.example.DtaAssigement.entity.TrendingFood;
import com.example.DtaAssigement.service.FacebookService;
import com.example.DtaAssigement.service.InstagramService;
import com.example.DtaAssigement.service.YouTubeService;
import com.example.DtaAssigement.service.TrendingFoodAnalyzerInstagram;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controller for Social Media API integrations (Facebook, Instagram, YouTube)
 */
@RestController
@RequestMapping("/api/social-media")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Slf4j
public class SocialMediaController {

    private final FacebookService facebookService;
    private final InstagramService instagramService;
    private final YouTubeService youtubeService;
    private final TrendingFoodAnalyzerInstagram trendingFoodAnalyzerInstagram;

    // ===== Test Endpoint =====

    @GetMapping("/test")
    public ResponseEntity<Map<String, String>> testConnection() {
        return ResponseEntity.ok(Map.of(
                "status", "ok",
                "message", "Social Media API is configured correctly",
                "timestamp", String.valueOf(System.currentTimeMillis())));
    }

    // ===== Facebook Endpoints =====

    /**
     * Get posts from a Facebook Page
     * 
     * @param pageId Facebook Page ID (e.g., "CocaCola" or numeric ID)
     * @param limit  Number of posts to fetch (default: 10)
     * @return List of Facebook posts
     */
    @GetMapping("/facebook/pages/{pageId}/posts")
    public ResponseEntity<List<FacebookPostDTO>> getFacebookPosts(
            @PathVariable String pageId,
            @RequestParam(defaultValue = "10") int limit) {

        log.info("GET /api/social-media/facebook/pages/{}/posts?limit={}", pageId, limit);

        List<FacebookPostDTO> posts = facebookService.getPagePosts(pageId, limit);

        if (posts.isEmpty()) {
            log.warn("No Facebook posts found for page ID: {}", pageId);
        }

        return ResponseEntity.ok(posts);
    }

    // ===== Instagram Endpoints =====

    /**
     * Get top posts for an Instagram hashtag
     * 
     * @param hashtag Hashtag name (without #, e.g., "pho" or "banhmi")
     * @param limit   Number of posts to fetch (default: 10)
     * @return List of Instagram posts
     */
    @GetMapping("/instagram/hashtag/{hashtag}/top")
    public ResponseEntity<List<InstagramPostDTO>> getInstagramTopPosts(
            @PathVariable String hashtag,
            @RequestParam(defaultValue = "10") int limit) {

        log.info("GET /api/social-media/instagram/hashtag/{}/top?limit={}", hashtag, limit);

        // First, search for the hashtag to get its ID
        String hashtagId = instagramService.searchHashtag(hashtag);

        if (hashtagId == null) {
            log.warn("Hashtag not found: #{}", hashtag);
            return ResponseEntity.notFound().build();
        }

        // Get top media for the hashtag
        List<InstagramPostDTO> posts = instagramService.getTopMedia(hashtagId, limit);

        if (posts.isEmpty()) {
            log.warn("No Instagram posts found for hashtag: #{}", hashtag);
        }

        return ResponseEntity.ok(posts);
    }

    /**
     * Get recent media (24h) for a hashtag
     */
    @GetMapping("/instagram/hashtag/{hashtag}/recent")
    public ResponseEntity<List<InstagramPostDTO>> getInstagramRecentPosts(
            @PathVariable String hashtag,
            @RequestParam(defaultValue = "50") int limit) {

        log.info("GET /api/social-media/instagram/hashtag/{}/recent?limit={}", hashtag, limit);

        // Get hashtag ID
        String hashtagId = instagramService.searchHashtag(hashtag);

        if (hashtagId == null) {
            log.warn("Hashtag not found: #{}", hashtag);
            return ResponseEntity.notFound().build();
        }

        // Get recent posts
        List<InstagramPostDTO> posts = instagramService.getRecentMedia(hashtagId, limit);

        if (posts.isEmpty()) {
            log.warn("No recent posts found for hashtag: #{}", hashtag);
            return ResponseEntity.ok(List.of());
        }

        log.info("Found {} recent posts for #{}", posts.size(), hashtag);
        return ResponseEntity.ok(posts);
    }

    /**
     * Get hashtag statistics
     */
    @GetMapping("/instagram/hashtag/{hashtag}/stats")
    public ResponseEntity<InstagramHashtagStatsDTO> getHashtagStats(@PathVariable String hashtag) {

        log.info("GET /api/social-media/instagram/hashtag/{}/stats", hashtag);

        InstagramHashtagStatsDTO stats = instagramService.calculateHashtagStats(hashtag);

        if (stats == null) {
            log.warn("Could not calculate stats for hashtag: #{}", hashtag);
            return ResponseEntity.notFound().build();
        }

        log.info("Stats for #{}: {} posts, avg engagement: {}",
                hashtag, stats.getTotalPosts(),
                stats.getAvgLikes() + stats.getAvgComments());

        return ResponseEntity.ok(stats);
    }

    /**
     * Get recently searched hashtags (last 7 days)
     * Returns hashtags that the Instagram Business Account has searched for
     * 
     * Note: Instagram allows max 30 unique hashtags to be queried within a 7-day
     * period
     * 
     * @param limit Maximum number of results (max: 30, default: 30)
     * @return List of recently searched hashtag IDs
     */
    @GetMapping("/instagram/hashtags/recent-searches")
    public ResponseEntity<List<InstagramRecentHashtagDTO>> getRecentlySearchedHashtags(
            @RequestParam(defaultValue = "30") int limit) {

        log.info("GET /api/social-media/instagram/hashtags/recent-searches?limit={}", limit);

        // Validate limit (Instagram API max is 30)
        if (limit > 30) {
            log.warn("Limit {} exceeds maximum of 30, using 30 instead", limit);
            limit = 30;
        }

        List<InstagramRecentHashtagDTO> hashtags = instagramService.getRecentlySearchedHashtags(limit);

        log.info("Found {} recently searched hashtags", hashtags.size());
        return ResponseEntity.ok(hashtags);
    }

    /**
     * Get hashtag details by ID
     * Returns basic information about a hashtag including its ID and name
     * 
     * @param hashtagId Instagram hashtag ID
     * @return Hashtag details (id and name)
     */
    @GetMapping("/instagram/hashtag/id/{hashtagId}")
    public ResponseEntity<InstagramHashtagDetailsDTO> getHashtagDetailsById(@PathVariable String hashtagId) {

        log.info("GET /api/social-media/instagram/hashtag/id/{}", hashtagId);

        InstagramHashtagDetailsDTO details = instagramService.getHashtagDetails(hashtagId);

        if (details == null) {
            log.warn("Hashtag not found for ID: {}", hashtagId);
            return ResponseEntity.notFound().build();
        }

        log.info("Found hashtag: #{} (ID: {})", details.getName(), details.getId());
        return ResponseEntity.ok(details);
    }

    /**
     * Get hot trending foods from Instagram
     * Analyzes hashtag engagement and growth rate to find viral foods
     * 
     * @param limit Number of top trending foods to return (default: 10, max: 20)
     * @return List of trending foods with engagement metrics
     */
    @GetMapping("/instagram/trending-foods")
    public ResponseEntity<List<InstagramTrendingFoodDTO>> getHotTrendingFoods(
            @RequestParam(defaultValue = "10") int limit) {

        log.info("GET /api/social-media/instagram/trending-foods?limit={}", limit);

        // Validate and cap limit
        if (limit > 20) {
            log.warn("Limit {} exceeds maximum of 20, using 20 instead", limit);
            limit = 20;
        }

        // Analyze and get top trending
        List<TrendingFood> trending = trendingFoodAnalyzerInstagram.analyzeHotTrending(limit);

        // Convert to DTO
        List<InstagramTrendingFoodDTO> response = trending.stream()
                .map(this::convertToInstagramTrendingDTO)
                .toList();

        log.info("Returning {} hot trending foods", response.size());
        return ResponseEntity.ok(response);
    }

    /**
     * Convert TrendingFood entity to InstagramTrendingFoodDTO
     */
    private InstagramTrendingFoodDTO convertToInstagramTrendingDTO(TrendingFood food) {
        return InstagramTrendingFoodDTO.builder()
                .id(food.getId())
                .name(capitalizeFood(food.getQuery()))
                .hashtag("#" + food.getQuery())
                .rank(food.getTrendScore())
                .score(food.getInstagramScore())
                .engagement(InstagramTrendingFoodDTO.EngagementMetrics.builder()
                        .average(food.getAvgEngagement())
                        .recent(food.getRecentEngagement())
                        .growthRate(food.getGrowthRate())
                        .posts24h(food.getTotalPosts24h())
                        .build())
                .trendStatus(getTrendEmoji(food.getStatus()) + " " + food.getStatus())
                .updatedAt(food.getLastUpdatedAt())
                .build();
    }

    /**
     * Get emoji for trend status
     */
    private String getTrendEmoji(String status) {
        return switch (status) {
            case "VIRAL" -> "🔥";
            case "TRENDING" -> "📈";
            case "STABLE" -> "➡️";
            case "DECLINING" -> "📉";
            default -> "❓";
        };
    }

    /**
     * Capitalize Vietnamese food names
     */
    private String capitalizeFood(String hashtag) {
        Map<String, String> foodNames = Map.of(
                "pho", "Phở",
                "banhmi", "Bánh mì",
                "buncha", "Bún chả",
                "comtam", "Cơm tấm",
                "banhxeo", "Bánh xèo",
                "goicuon", "Gỏi cuốn",
                "bunbo", "Bún bò",
                "cacom", "Cá kho",
                "miqua", "Mì quảng",
                "banhcuon", "Bánh cuốn");
        return foodNames.getOrDefault(hashtag, hashtag);
    }

    // ===== YouTube Endpoints =====

    /**
     * Search YouTube videos by keyword
     * 
     * @param keyword Search query (e.g., "phở", "bánh mì")
     * @param region  Region code (default: "VN")
     * @param limit   Number of results (default: 20)
     * @return List of search results
     */
    @GetMapping("/youtube/search")
    public ResponseEntity<List<YouTubeSearchResultDTO>> searchYouTubeVideos(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "VN") String region,
            @RequestParam(defaultValue = "20") int limit) {

        log.info("GET /api/social-media/youtube/search?keyword={}&region={}&limit={}", keyword, region, limit);

        List<YouTubeSearchResultDTO> results = youtubeService.searchVideos(keyword, region, limit);

        return ResponseEntity.ok(results);
    }

    /**
     * Get trending YouTube videos
     * 
     * @param region     Region code (default: "VN")
     * @param categoryId Optional category ID (e.g., "26" for Howto & Style)
     * @param limit      Number of results (default: 20)
     * @return List of trending videos
     */
    @GetMapping("/youtube/trending")
    public ResponseEntity<List<YouTubeVideoDTO>> getTrendingYouTubeVideos(
            @RequestParam(defaultValue = "VN") String region,
            @RequestParam(required = false) String categoryId,
            @RequestParam(defaultValue = "20") int limit) {

        log.info("GET /api/social-media/youtube/trending?region={}&categoryId={}&limit={}", region, categoryId, limit);

        List<YouTubeVideoDTO> videos = youtubeService.getTrendingVideos(region, categoryId, limit);

        return ResponseEntity.ok(videos);
    }

    /**
     * Get YouTube video details
     * 
     * @param videoId YouTube video ID
     * @return Video details
     */
    @GetMapping("/youtube/videos/{videoId}")
    public ResponseEntity<YouTubeVideoDTO> getYouTubeVideoDetails(@PathVariable String videoId) {

        log.info("GET /api/social-media/youtube/videos/{}", videoId);

        YouTubeVideoDTO video = youtubeService.getVideoDetails(videoId);

        if (video == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(video);
    }

    /**
     * Get YouTube video categories for a region
     * 
     * @param region Region code (default: "VN")
     * @return List of categories
     */
    @GetMapping("/youtube/categories")
    public ResponseEntity<List<YouTubeCategoryDTO>> getYouTubeCategories(
            @RequestParam(defaultValue = "VN") String region) {

        log.info("GET /api/social-media/youtube/categories?region={}", region);

        List<YouTubeCategoryDTO> categories = youtubeService.getCategories(region);

        return ResponseEntity.ok(categories);
    }

    /**
     * Get comments for a YouTube video
     * 
     * @param videoId YouTube video ID
     * @param limit   Number of comments (default: 100)
     * @return List of comments
     */
    @GetMapping("/youtube/videos/{videoId}/comments")
    public ResponseEntity<List<YouTubeCommentDTO>> getYouTubeVideoComments(
            @PathVariable String videoId,
            @RequestParam(defaultValue = "100") int limit) {

        log.info("GET /api/social-media/youtube/videos/{}/comments?limit={}", videoId, limit);

        List<YouTubeCommentDTO> comments = youtubeService.getVideoComments(videoId, limit);

        return ResponseEntity.ok(comments);
    }
}
