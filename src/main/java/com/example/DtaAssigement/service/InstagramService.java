package com.example.DtaAssigement.service;

import com.example.DtaAssigement.config.SocialMediaConfig;
import com.example.DtaAssigement.dto.InstagramPostDTO;
import com.example.DtaAssigement.dto.InstagramHashtagStatsDTO;
import com.example.DtaAssigement.dto.InstagramRecentHashtagDTO;
import com.example.DtaAssigement.dto.InstagramHashtagDetailsDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for Instagram Graph API operations
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class InstagramService {

    @Qualifier("socialMediaRestTemplate")
    private final RestTemplate restTemplate;

    private final SocialMediaConfig config;

    private static final String INSTAGRAM_API = "https://graph.facebook.com/v21.0";

    /**
     * Search hashtag to get hashtag ID
     * 
     * @param hashtag Hashtag name (without #)
     * @return Hashtag ID or null if not found
     */
    @SuppressWarnings("unchecked")
    public String searchHashtag(String hashtag) {
        String accessToken = config.getInstagramAccessToken();
        String businessAccountId = config.getInstagramBusinessAccountId();

        if (accessToken == null || accessToken.isEmpty() || businessAccountId == null || businessAccountId.isEmpty()) {
            log.error("Instagram credentials not configured. Cannot search hashtag.");
            return null;
        }

        String url = String.format(
                "%s/ig_hashtag_search?user_id=%s&q=%s&access_token=%s",
                INSTAGRAM_API, businessAccountId, hashtag, accessToken);

        try {
            log.debug("Searching Instagram hashtag: {}", hashtag);

            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            if (response != null && response.containsKey("data")) {
                List<Map<String, Object>> data = (List<Map<String, Object>>) response.get("data");

                if (data != null && !data.isEmpty()) {
                    String hashtagId = data.get(0).get("id").toString();
                    log.info("Found Instagram hashtag ID for #{}: {}", hashtag, hashtagId);
                    return hashtagId;
                }
            }

            log.warn("No hashtag found for: {}", hashtag);
            return null;

        } catch (Exception e) {
            log.error("Error searching Instagram hashtag {}: {}", hashtag, e.getMessage());
            return null;
        }
    }

    /**
     * Get top media posts for a hashtag
     * 
     * @param hashtagId Instagram hashtag ID
     * @param limit     Number of posts to fetch
     * @return List of Instagram posts
     */
    @SuppressWarnings("unchecked")
    public List<InstagramPostDTO> getTopMedia(String hashtagId, int limit) {
        String accessToken = config.getInstagramAccessToken();
        String businessAccountId = config.getInstagramBusinessAccountId();

        if (accessToken == null || accessToken.isEmpty() || businessAccountId == null || businessAccountId.isEmpty()) {
            log.error("Instagram credentials not configured. Cannot fetch media.");
            return List.of();
        }

        String url = String.format(
                "%s/%s/top_media?user_id=%s&fields=id,caption,media_type,media_url,permalink,timestamp,like_count,comments_count&limit=%d&access_token=%s",
                INSTAGRAM_API, hashtagId, businessAccountId, limit, accessToken);

        try {
            log.debug("Fetching top {} media for hashtag ID: {}", limit, hashtagId);

            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            if (response != null && response.containsKey("data")) {
                List<Map<String, Object>> data = (List<Map<String, Object>>) response.get("data");

                List<InstagramPostDTO> result = data.stream()
                        .map(this::convertToDTO)
                        .toList();

                log.info("Successfully fetched {} Instagram posts for hashtag ID: {}", result.size(), hashtagId);
                return result;
            }

            return List.of();

        } catch (Exception e) {
            log.error("Error getting Instagram top media for hashtag {}: {}", hashtagId, e.getMessage());
            return List.of();
        }
    }

    /**
     * Convert Instagram API response to DTO
     */
    private InstagramPostDTO convertToDTO(Map<String, Object> post) {
        return InstagramPostDTO.builder()
                .id((String) post.get("id"))
                .caption((String) post.get("caption"))
                .mediaType((String) post.get("media_type"))
                .mediaUrl((String) post.get("media_url"))
                .permalink((String) post.get("permalink"))
                .timestamp((String) post.get("timestamp"))
                .likeCount(post.get("like_count") != null ? (Integer) post.get("like_count") : 0)
                .commentsCount(post.get("comments_count") != null ? (Integer) post.get("comments_count") : 0)
                .build();
    }

    /**
     * Get recent media for hashtag
     * Official API: /{hashtag-id}/recent_media
     * Returns posts from last 24 hours
     */
    @SuppressWarnings("unchecked")
    public List<InstagramPostDTO> getRecentMedia(String hashtagId, int limit) {
        String accessToken = config.getInstagramAccessToken();
        String businessAccountId = config.getInstagramBusinessAccountId();

        if (accessToken == null || accessToken.isEmpty() || businessAccountId == null || businessAccountId.isEmpty()) {
            log.error("Instagram credentials not configured. Cannot fetch recent media.");
            return List.of();
        }

        String url = String.format(
                "%s/%s/recent_media?user_id=%s&fields=id,caption,media_type,media_url,permalink,timestamp,like_count,comments_count&limit=%d&access_token=%s",
                INSTAGRAM_API, hashtagId, businessAccountId, limit, accessToken);

        try {
            log.debug("Fetching recent {} media for hashtag ID: {}", limit, hashtagId);

            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            if (response != null && response.containsKey("data")) {
                List<Map<String, Object>> data = (List<Map<String, Object>>) response.get("data");

                List<InstagramPostDTO> result = data.stream()
                        .map(this::convertToDTO)
                        .toList();

                log.info("Successfully fetched {} recent posts for hashtag ID: {}", result.size(), hashtagId);
                return result;
            }

            return List.of();

        } catch (Exception e) {
            log.error("Error getting recent media for hashtag {}: {}", hashtagId, e.getMessage());
            return List.of();
        }
    }

    /**
     * Get hashtags that the business account has searched in the last 7 days
     * Official API: /{ig-user-id}/recently_searched_hashtags
     * 
     * Note: Instagram allows max 30 unique hashtags to be queried within a rolling
     * 7-day period.
     * A hashtag only counts once when first searched, subsequent searches don't
     * count.
     * 
     * @param limit Maximum number of results (max: 30, default: 25)
     * @return List of recently searched hashtag IDs
     */
    @SuppressWarnings("unchecked")
    public List<InstagramRecentHashtagDTO> getRecentlySearchedHashtags(int limit) {
        String accessToken = config.getInstagramAccessToken();
        String businessAccountId = config.getInstagramBusinessAccountId();

        if (accessToken == null || accessToken.isEmpty() || businessAccountId == null || businessAccountId.isEmpty()) {
            log.error("Instagram credentials not configured. Cannot fetch recent searches.");
            return List.of();
        }

        String url = String.format(
                "%s/%s/recently_searched_hashtags?limit=%d&access_token=%s",
                INSTAGRAM_API, businessAccountId, limit, accessToken);

        try {
            log.debug("Fetching recently searched hashtags (limit: {})", limit);

            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            if (response != null && response.containsKey("data")) {
                List<Map<String, Object>> data = (List<Map<String, Object>>) response.get("data");

                List<InstagramRecentHashtagDTO> result = data.stream()
                        .map(item -> InstagramRecentHashtagDTO.builder()
                                .id((String) item.get("id"))
                                .build())
                        .toList();

                log.info("Found {} recently searched hashtags", result.size());
                return result;
            }

            return List.of();

        } catch (Exception e) {
            log.error("Error getting recently searched hashtags: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * Get hashtag details by hashtag ID
     * Official API: GET /{ig-hashtag-id}?fields=id,name
     * 
     * @param hashtagId Instagram hashtag ID
     * @return Hashtag details (id and name) or null if not found
     */
    @SuppressWarnings("unchecked")
    public InstagramHashtagDetailsDTO getHashtagDetails(String hashtagId) {
        String accessToken = config.getInstagramAccessToken();

        if (accessToken == null || accessToken.isEmpty()) {
            log.error("Instagram credentials not configured. Cannot fetch hashtag details.");
            return null;
        }

        String url = String.format(
                "%s/%s?fields=id,name&access_token=%s",
                INSTAGRAM_API, hashtagId, accessToken);

        try {
            log.debug("Fetching hashtag details for ID: {}", hashtagId);

            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            if (response != null) {
                InstagramHashtagDetailsDTO details = InstagramHashtagDetailsDTO.builder()
                        .id((String) response.get("id"))
                        .name((String) response.get("name"))
                        .build();

                log.info("Found hashtag details: #{} (ID: {})", details.getName(), details.getId());
                return details;
            }

            return null;

        } catch (Exception e) {
            log.error("Error getting hashtag details for ID {}: {}", hashtagId, e.getMessage());
            return null;
        }
    }

    /**
     * Calculate basic stats for a hashtag
     * Based on recent posts only (Graph API limitation)
     */
    public InstagramHashtagStatsDTO calculateHashtagStats(String hashtag) {
        log.info("Calculating stats for hashtag: #{}", hashtag);

        // 1. Get hashtag ID
        String hashtagId = searchHashtag(hashtag);
        if (hashtagId == null) {
            log.warn("Hashtag not found: #{}", hashtag);
            return null;
        }

        // 2. Get recent posts (max 15 to avoid API rate limit)
        List<InstagramPostDTO> recentPosts = getRecentMedia(hashtagId, 15);

        if (recentPosts.isEmpty()) {
            log.warn("No recent posts for hashtag: #{}", hashtag);
            return InstagramHashtagStatsDTO.builder()
                    .hashtag(hashtag)
                    .hashtagId(hashtagId)
                    .totalPosts(0)
                    .avgLikes(0.0)
                    .avgComments(0.0)
                    .totalLikes(0)
                    .totalComments(0)
                    .analyzedAt(LocalDateTime.now())
                    .build();
        }

        // 3. Calculate stats
        double avgLikes = recentPosts.stream()
                .mapToInt(InstagramPostDTO::getLikeCount)
                .average()
                .orElse(0);

        double avgComments = recentPosts.stream()
                .mapToInt(InstagramPostDTO::getCommentsCount)
                .average()
                .orElse(0);

        int totalLikes = recentPosts.stream()
                .mapToInt(InstagramPostDTO::getLikeCount)
                .sum();

        int totalComments = recentPosts.stream()
                .mapToInt(InstagramPostDTO::getCommentsCount)
                .sum();

        InstagramHashtagStatsDTO stats = InstagramHashtagStatsDTO.builder()
                .hashtag(hashtag)
                .hashtagId(hashtagId)
                .totalPosts(recentPosts.size())
                .avgLikes(avgLikes)
                .avgComments(avgComments)
                .totalLikes(totalLikes)
                .totalComments(totalComments)
                .analyzedAt(LocalDateTime.now())
                .build();

        log.info("Stats for #{}: {} posts, avg engagement: {:.1f}",
                hashtag, stats.getTotalPosts(), avgLikes + avgComments);

        return stats;
    }

    /**
     * Filter posts by hours ago
     */
    public List<InstagramPostDTO> filterPostsByHours(List<InstagramPostDTO> posts, int hoursAgo) {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(hoursAgo);

        return posts.stream()
                .filter(post -> {
                    try {
                        // Parse timestamp: "2024-01-28T15:30:00+0000"
                        String timestamp = post.getTimestamp();
                        if (timestamp != null && timestamp.length() >= 19) {
                            LocalDateTime postTime = LocalDateTime.parse(
                                    timestamp.substring(0, 19),
                                    DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                            return postTime.isAfter(cutoff);
                        }
                        return false;
                    } catch (Exception e) {
                        log.debug("Failed to parse timestamp: {}", post.getTimestamp());
                        return false;
                    }
                })
                .collect(Collectors.toList());
    }

    /**
     * Check if posts contain location keywords
     */
    public boolean hasLocationKeywords(List<InstagramPostDTO> posts, String... keywords) {
        if (posts.isEmpty()) {
            return false;
        }

        long matchCount = posts.stream()
                .filter(p -> {
                    String caption = (p.getCaption() != null ? p.getCaption() : "").toLowerCase();
                    return Arrays.stream(keywords)
                            .anyMatch(keyword -> caption.contains(keyword.toLowerCase()));
                })
                .count();

        return matchCount > 0;
    }
}
