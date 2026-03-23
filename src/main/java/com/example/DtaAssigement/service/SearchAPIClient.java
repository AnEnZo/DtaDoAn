package com.example.DtaAssigement.service;

import com.example.DtaAssigement.dto.searchapi.AutocompleteResponse;
import com.example.DtaAssigement.dto.searchapi.TrendingNewsResponse;
import com.example.DtaAssigement.dto.searchapi.TrendingNowResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Client for SearchAPI.io Google Trends APIs
 */
@Service
@Slf4j
public class SearchAPIClient {

    @Value("${searchapi.api-key:}")
    private String apiKey;

    @Value("${searchapi.base-url:https://www.searchapi.io/api/v1/search}")
    private String baseUrl;

    private final RestTemplate restTemplate;

    public SearchAPIClient() {
        this.restTemplate = new RestTemplate();
    }

    /**
     * Get trending topics from Google Trends
     * 
     * @param geo      Geographic location (e.g., "VN", "US")
     * @param time     Time window (e.g., "past_24_hours", "past_7_days")
     * @param category Category filter (e.g., "food_and_drink")
     * @return TrendingNowResponse
     */
    public TrendingNowResponse getTrendingNow(String geo, String time, String category) {
        log.debug("Fetching trending topics: geo={}, time={}, category={}", geo, time, category);

        String url = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .queryParam("engine", "google_trends_trending_now")
                .queryParam("geo", geo)
                .queryParam("time", time)
                .queryParam("api_key", apiKey)
                .toUriString();

        try {
            ResponseEntity<TrendingNowResponse> response = restTemplate.getForEntity(
                    url,
                    TrendingNowResponse.class);

            log.info("Successfully fetched {} trending topics",
                    response.getBody() != null && response.getBody().getTrends() != null
                            ? response.getBody().getTrends().size()
                            : 0);

            return response.getBody();

        } catch (Exception e) {
            log.error("Error fetching trending topics from SearchAPI: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to fetch trending data from SearchAPI", e);
        }
    }

    /**
     * Get news articles for a trending topic
     * 
     * @param newsToken Token from trending topic
     * @return TrendingNewsResponse
     */
    public TrendingNewsResponse getTrendingNews(String newsToken) {
        log.debug("Fetching trending news for token: {}", newsToken);

        String url = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .queryParam("engine", "google_trends_trending_now_news")
                .queryParam("news_token", newsToken)
                .queryParam("api_key", apiKey)
                .toUriString();

        try {
            ResponseEntity<TrendingNewsResponse> response = restTemplate.getForEntity(
                    url,
                    TrendingNewsResponse.class);

            log.info("Successfully fetched {} news articles",
                    response.getBody() != null && response.getBody().getArticles() != null
                            ? response.getBody().getArticles().size()
                            : 0);

            return response.getBody();

        } catch (Exception e) {
            log.error("Error fetching trending news from SearchAPI: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to fetch trending news from SearchAPI", e);
        }
    }

    /**
     * Get autocomplete suggestions
     * 
     * @param query Search query (minimum 3 characters)
     * @param geo   Geographic location
     * @return AutocompleteResponse
     */
    public AutocompleteResponse getAutocomplete(String query, String geo) {
        log.debug("Fetching autocomplete for query: {}", query);

        if (query == null || query.length() < 3) {
            log.warn("Query too short for autocomplete: {}", query);
            return new AutocompleteResponse(); // Return empty response
        }

        String url = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .queryParam("engine", "google_trends_autocomplete")
                .queryParam("q", query)
                .queryParam("geo", geo)
                .queryParam("api_key", apiKey)
                .toUriString();

        try {
            ResponseEntity<AutocompleteResponse> response = restTemplate.getForEntity(
                    url,
                    AutocompleteResponse.class);

            log.debug("Successfully fetched autocomplete suggestions");
            return response.getBody();

        } catch (Exception e) {
            log.error("Error fetching autocomplete from SearchAPI: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to fetch autocomplete from SearchAPI", e);
        }
    }

    /**
     * Get raw trending topics (pass-through)
     */
    public Object getRawTrendingNow(String geo, String time, String category) {
        String url = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .queryParam("engine", "google_trends_trending_now")
                .queryParam("geo", geo)
                .queryParam("time", time)
                .queryParam("api_key", apiKey)
                .toUriString();

        try {
            return restTemplate.getForObject(url, Object.class);
        } catch (Exception e) {
            log.error("Error fetching raw trending topics", e);
            throw new RuntimeException("Failed to fetch raw trending data", e);
        }
    }

    /**
     * Get raw trending news (pass-through)
     */
    public Object getRawTrendingNews(String newsToken) {
        String url = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .queryParam("engine", "google_trends_trending_now_news")
                .queryParam("news_token", newsToken)
                .queryParam("api_key", apiKey)
                .toUriString();

        try {
            return restTemplate.getForObject(url, Object.class);
        } catch (Exception e) {
            log.error("Error fetching raw trending news", e);
            throw new RuntimeException("Failed to fetch raw trending news", e);
        }
    }

    /**
     * Get raw autocomplete (pass-through)
     */
    public Object getRawAutocomplete(String query, String geo) {
        String url = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .queryParam("engine", "google_trends_autocomplete")
                .queryParam("q", query)
                .queryParam("geo", geo)
                .queryParam("api_key", apiKey)
                .toUriString();

        try {
            return restTemplate.getForObject(url, Object.class);
        } catch (Exception e) {
            log.error("Error fetching raw autocomplete", e);
            throw new RuntimeException("Failed to fetch raw autocomplete", e);
        }
    }

    /**
     * Get raw google rank tracking (pass-through)
     */
    public Object getRawGoogleRank(String query, String location) {
        String url = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .queryParam("engine", "google_rank_tracking")
                .queryParam("q", query)
                .queryParam("location", location)
                .queryParam("api_key", apiKey)
                .toUriString();

        try {
            return restTemplate.getForObject(url, Object.class);
        } catch (Exception e) {
            log.error("Error fetching raw google rank", e);
            throw new RuntimeException("Failed to fetch raw google rank", e);
        }
    }
}
