package com.example.DtaAssigement.service;

import com.example.DtaAssigement.config.SocialMediaConfig;
import com.example.DtaAssigement.dto.YouTubeCategoryDTO;
import com.example.DtaAssigement.dto.YouTubeCommentDTO;
import com.example.DtaAssigement.dto.YouTubeSearchResultDTO;
import com.example.DtaAssigement.dto.YouTubeVideoDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Service for YouTube Data API v3 operations
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class YouTubeService {

    @Qualifier("socialMediaRestTemplate")
    private final RestTemplate restTemplate;

    private final SocialMediaConfig config;

    private static final String YOUTUBE_API_BASE = "https://www.googleapis.com/youtube/v3";

    /**
     * Search videos by keyword
     * 
     * @param keyword    Search query
     * @param regionCode Region code (e.g., "VN")
     * @param maxResults Max results
     * @return List of search results
     */
    @SuppressWarnings("unchecked")
    public List<YouTubeSearchResultDTO> searchVideos(String keyword, String regionCode, int maxResults) {
        String apiKey = config.getYoutubeApiKey();
        if (apiKey == null || apiKey.isEmpty()) {
            log.error("YouTube API key not configured");
            return List.of();
        }

        try {
            String url = UriComponentsBuilder.fromHttpUrl(YOUTUBE_API_BASE + "/search")
                    .queryParam("part", "snippet")
                    .queryParam("q", keyword)
                    .queryParam("type", "video")
                    .queryParam("order", "viewCount")
                    .queryParam("regionCode", regionCode)
                    .queryParam("maxResults", maxResults)
                    .queryParam("key", apiKey)
                    .toUriString();

            log.info("YouTube search request: keyword={}, region={}, limit={}", keyword, regionCode, maxResults);
            log.debug("YouTube API URL: {}", url);

            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            log.debug("YouTube API Response: {}", response);

            if (response != null && response.containsKey("error")) {
                Map<String, Object> error = (Map<String, Object>) response.get("error");
                log.error("YouTube API Error: code={}, message={}", error.get("code"), error.get("message"));
                log.error("Full error details: {}", error);
                return List.of();
            }

            if (response != null && response.containsKey("items")) {
                List<Map<String, Object>> items = (List<Map<String, Object>>) response.get("items");
                List<YouTubeSearchResultDTO> results = new ArrayList<>();

                for (Map<String, Object> item : items) {
                    Map<String, Object> id = (Map<String, Object>) item.get("id");
                    Map<String, Object> snippet = (Map<String, Object>) item.get("snippet");

                    if (id != null && snippet != null) {
                        Map<String, Object> thumbnails = (Map<String, Object>) snippet.get("thumbnails");
                        Map<String, Object> defaultThumb = thumbnails != null
                                ? (Map<String, Object>) thumbnails.get("medium")
                                : null;

                        results.add(YouTubeSearchResultDTO.builder()
                                .videoId((String) id.get("videoId"))
                                .title((String) snippet.get("title"))
                                .description((String) snippet.get("description"))
                                .channelTitle((String) snippet.get("channelTitle"))
                                .channelId((String) snippet.get("channelId"))
                                .publishedAt((String) snippet.get("publishedAt"))
                                .thumbnailUrl(defaultThumb != null ? (String) defaultThumb.get("url") : null)
                                .build());
                    }
                }

                log.info("Found {} YouTube videos for keyword: {}", results.size(), keyword);
                return results;
            }

            log.warn("YouTube API returned null or no items. Response: {}", response);
            return List.of();

        } catch (Exception e) {
            log.error("Error searching YouTube videos: {}", e.getMessage(), e);
            log.error("Exception class: {}", e.getClass().getName());
            if (e.getCause() != null) {
                log.error("Cause: {}", e.getCause().getMessage());
            }
            return List.of();
        }
    }

    /**
     * Get trending videos (most popular)
     * 
     * @param regionCode Region code
     * @param categoryId Optional category ID
     * @param maxResults Max results
     * @return List of trending videos
     */
    @SuppressWarnings("unchecked")
    public List<YouTubeVideoDTO> getTrendingVideos(String regionCode, String categoryId, int maxResults) {
        String apiKey = config.getYoutubeApiKey();
        if (apiKey == null || apiKey.isEmpty()) {
            log.error("YouTube API key not configured");
            return List.of();
        }

        try {
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(YOUTUBE_API_BASE + "/videos")
                    .queryParam("part", "snippet,statistics,contentDetails")
                    .queryParam("chart", "mostPopular")
                    .queryParam("regionCode", regionCode)
                    .queryParam("maxResults", maxResults);

            if (categoryId != null && !categoryId.isEmpty()) {
                builder.queryParam("videoCategoryId", categoryId);
            }

            builder.queryParam("key", apiKey);

            String url = builder.toUriString();

            log.debug("YouTube trending request: region={}, category={}, limit={}", regionCode, categoryId,
                    maxResults);

            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            if (response != null && response.containsKey("items")) {
                List<Map<String, Object>> items = (List<Map<String, Object>>) response.get("items");
                return items.stream()
                        .map(this::mapToVideoDTO)
                        .toList();
            }

            return List.of();

        } catch (Exception e) {
            log.error("Error getting trending YouTube videos: {}", e.getMessage(), e);
            return List.of();
        }
    }

    /**
     * Get video details by ID
     * 
     * @param videoId Video ID
     * @return Video details
     */
    @SuppressWarnings("unchecked")
    public YouTubeVideoDTO getVideoDetails(String videoId) {
        String apiKey = config.getYoutubeApiKey();
        if (apiKey == null || apiKey.isEmpty()) {
            log.error("YouTube API key not configured");
            return null;
        }

        try {
            String url = UriComponentsBuilder.fromHttpUrl(YOUTUBE_API_BASE + "/videos")
                    .queryParam("part", "snippet,statistics,contentDetails")
                    .queryParam("id", videoId)
                    .queryParam("key", apiKey)
                    .toUriString();

            log.debug("YouTube video details request: videoId={}", videoId);

            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            if (response != null && response.containsKey("items")) {
                List<Map<String, Object>> items = (List<Map<String, Object>>) response.get("items");
                if (!items.isEmpty()) {
                    return mapToVideoDTO(items.get(0));
                }
            }

            log.warn("Video not found: {}", videoId);
            return null;

        } catch (Exception e) {
            log.error("Error getting YouTube video details: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Get video categories for a region
     * 
     * @param regionCode Region code
     * @return List of categories
     */
    @SuppressWarnings("unchecked")
    public List<YouTubeCategoryDTO> getCategories(String regionCode) {
        String apiKey = config.getYoutubeApiKey();
        if (apiKey == null || apiKey.isEmpty()) {
            log.error("YouTube API key not configured");
            return List.of();
        }

        try {
            String url = UriComponentsBuilder.fromHttpUrl(YOUTUBE_API_BASE + "/videoCategories")
                    .queryParam("part", "snippet")
                    .queryParam("regionCode", regionCode)
                    .queryParam("key", apiKey)
                    .toUriString();

            log.debug("YouTube categories request: region={}", regionCode);

            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            if (response != null && response.containsKey("items")) {
                List<Map<String, Object>> items = (List<Map<String, Object>>) response.get("items");
                List<YouTubeCategoryDTO> categories = new ArrayList<>();

                for (Map<String, Object> item : items) {
                    Map<String, Object> snippet = (Map<String, Object>) item.get("snippet");
                    if (snippet != null) {
                        categories.add(YouTubeCategoryDTO.builder()
                                .id((String) item.get("id"))
                                .title((String) snippet.get("title"))
                                .assignable((Boolean) snippet.get("assignable"))
                                .build());
                    }
                }

                log.info("Found {} YouTube categories for region {}", categories.size(), regionCode);
                return categories;
            }

            return List.of();

        } catch (Exception e) {
            log.error("Error getting YouTube categories: {}", e.getMessage(), e);
            return List.of();
        }
    }

    /**
     * Get comments for a video
     * 
     * @param videoId    Video ID
     * @param maxResults Max results
     * @return List of comments
     */
    @SuppressWarnings("unchecked")
    public List<YouTubeCommentDTO> getVideoComments(String videoId, int maxResults) {
        String apiKey = config.getYoutubeApiKey();
        if (apiKey == null || apiKey.isEmpty()) {
            log.error("YouTube API key not configured");
            return List.of();
        }

        try {
            String url = UriComponentsBuilder.fromHttpUrl(YOUTUBE_API_BASE + "/commentThreads")
                    .queryParam("part", "snippet")
                    .queryParam("videoId", videoId)
                    .queryParam("order", "relevance")
                    .queryParam("maxResults", maxResults)
                    .queryParam("key", apiKey)
                    .toUriString();

            log.debug("YouTube comments request: videoId={}, limit={}", videoId, maxResults);

            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            if (response != null && response.containsKey("items")) {
                List<Map<String, Object>> items = (List<Map<String, Object>>) response.get("items");
                List<YouTubeCommentDTO> comments = new ArrayList<>();

                for (Map<String, Object> item : items) {
                    Map<String, Object> snippet = (Map<String, Object>) item.get("snippet");
                    if (snippet != null) {
                        Map<String, Object> topComment = (Map<String, Object>) snippet.get("topLevelComment");
                        if (topComment != null) {
                            Map<String, Object> commentSnippet = (Map<String, Object>) topComment.get("snippet");
                            if (commentSnippet != null) {
                                comments.add(YouTubeCommentDTO.builder()
                                        .commentId((String) topComment.get("id"))
                                        .authorName((String) commentSnippet.get("authorDisplayName"))
                                        .authorChannelId((String) commentSnippet.get("authorChannelId"))
                                        .textDisplay((String) commentSnippet.get("textDisplay"))
                                        .likeCount(commentSnippet.get("likeCount") != null
                                                ? (Integer) commentSnippet.get("likeCount")
                                                : 0)
                                        .publishedAt((String) commentSnippet.get("publishedAt"))
                                        .replyCount(snippet.get("totalReplyCount") != null
                                                ? (Integer) snippet.get("totalReplyCount")
                                                : 0)
                                        .build());
                            }
                        }
                    }
                }

                log.info("Found {} comments for video {}", comments.size(), videoId);
                return comments;
            }

            return List.of();

        } catch (Exception e) {
            log.error("Error getting YouTube video comments: {}", e.getMessage(), e);
            return List.of();
        }
    }

    /**
     * Helper method to map API response to YouTubeVideoDTO
     */
    @SuppressWarnings("unchecked")
    private YouTubeVideoDTO mapToVideoDTO(Map<String, Object> item) {
        Map<String, Object> snippet = (Map<String, Object>) item.get("snippet");
        Map<String, Object> statistics = (Map<String, Object>) item.get("statistics");
        Map<String, Object> contentDetails = (Map<String, Object>) item.get("contentDetails");

        Map<String, Object> thumbnails = snippet != null ? (Map<String, Object>) snippet.get("thumbnails") : null;
        Map<String, Object> mediumThumb = thumbnails != null ? (Map<String, Object>) thumbnails.get("medium") : null;

        return YouTubeVideoDTO.builder()
                .id((String) item.get("id"))
                .title(snippet != null ? (String) snippet.get("title") : null)
                .description(snippet != null ? (String) snippet.get("description") : null)
                .channelTitle(snippet != null ? (String) snippet.get("channelTitle") : null)
                .channelId(snippet != null ? (String) snippet.get("channelId") : null)
                .thumbnailUrl(mediumThumb != null ? (String) mediumThumb.get("url") : null)
                .publishedAt(snippet != null ? (String) snippet.get("publishedAt") : null)
                .categoryId(snippet != null ? (String) snippet.get("categoryId") : null)
                .viewCount(statistics != null && statistics.get("viewCount") != null
                        ? Long.parseLong(statistics.get("viewCount").toString())
                        : 0L)
                .likeCount(statistics != null && statistics.get("likeCount") != null
                        ? Long.parseLong(statistics.get("likeCount").toString())
                        : 0L)
                .commentCount(statistics != null && statistics.get("commentCount") != null
                        ? Long.parseLong(statistics.get("commentCount").toString())
                        : 0L)
                .duration(contentDetails != null ? (String) contentDetails.get("duration") : null)
                .definition(contentDetails != null ? (String) contentDetails.get("definition") : null)
                .build();
    }
}
