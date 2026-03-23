package com.example.DtaAssigement.service;

import com.example.DtaAssigement.dto.serpapi.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Client for SerpAPI - Google Trends and Baidu APIs
 */
@Service
@Slf4j
public class SerpAPIClient {

    @Value("${serpapi.api-key:}")
    private String apiKey;

    @Value("${serpapi.base-url:https://serpapi.com/search.json}")
    private String baseUrl;

    private final RestTemplate restTemplate;

    public SerpAPIClient() {
        this.restTemplate = new RestTemplate();
    }

    /**
     * Get Google Trends Trending Now
     */
    public SerpTrendingNowResponse getTrendingNow(String geo, Integer hours, Integer categoryId, String hl) {
        String url = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .queryParam("engine", "google_trends_trending_now")
                .queryParam("geo", geo)
                .queryParam("hours", hours)
                .queryParam("hl", hl)
                .queryParam("api_key", apiKey)
                .toUriString();

        if (categoryId != null) {
            url = UriComponentsBuilder.fromHttpUrl(url)
                    .queryParam("category_id", categoryId)
                    .toUriString();
        }

        try {
            log.debug("Calling SerpAPI Trending Now: geo={}, hours={}", geo, hours);
            return restTemplate.getForObject(url, SerpTrendingNowResponse.class);
        } catch (Exception e) {
            log.error("Error calling SerpAPI Trending Now: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to fetch trending now from SerpAPI", e);
        }
    }

    /**
     * Get Google Trends Interest Over Time
     */
    public SerpInterestOverTimeResponse getInterestOverTime(String q, String geo, String date, String hl) {
        String url = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .queryParam("engine", "google_trends")
                .queryParam("data_type", "TIMESERIES")
                .queryParam("q", q)
                .queryParam("geo", geo)
                .queryParam("date", date)
                .queryParam("hl", hl)
                .queryParam("api_key", apiKey)
                .toUriString();

        try {
            log.debug("Calling SerpAPI Interest Over Time: q={}, geo={}", q, geo);
            return restTemplate.getForObject(url, SerpInterestOverTimeResponse.class);
        } catch (Exception e) {
            log.error("Error calling SerpAPI Interest Over Time: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to fetch interest over time from SerpAPI", e);
        }
    }

    /**
     * Get Google Trends News
     */
    public SerpTrendingNewsResponse getTrendingNews(String newsToken) {
        String url = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .queryParam("engine", "google_trends_trending_now_news")
                .queryParam("news_token", newsToken)
                .queryParam("api_key", apiKey)
                .toUriString();

        try {
            log.debug("Calling SerpAPI Trending News: newsToken={}", newsToken);
            return restTemplate.getForObject(url, SerpTrendingNewsResponse.class);
        } catch (Exception e) {
            log.error("Error calling SerpAPI Trending News: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to fetch trending news from SerpAPI", e);
        }
    }

    /**
     * Get Google Trends Related Queries
     */
    public SerpRelatedQueriesResponse getRelatedQueries(String q, String geo, String date, String hl) {
        String url = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .queryParam("engine", "google_trends")
                .queryParam("data_type", "RELATED_QUERIES")
                .queryParam("q", q)
                .queryParam("geo", geo)
                .queryParam("date", date)
                .queryParam("hl", hl)
                .queryParam("api_key", apiKey)
                .toUriString();

        try {
            log.debug("Calling SerpAPI Related Queries: q={}, geo={}", q, geo);
            return restTemplate.getForObject(url, SerpRelatedQueriesResponse.class);
        } catch (Exception e) {
            log.error("Error calling SerpAPI Related Queries: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to fetch related queries from SerpAPI", e);
        }
    }

    /**
     * Get Google Trends Related Topics
     */
    public SerpRelatedTopicsResponse getRelatedTopics(String q, String geo, String date, String hl) {
        String url = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .queryParam("engine", "google_trends")
                .queryParam("data_type", "RELATED_TOPICS")
                .queryParam("q", q)
                .queryParam("geo", geo)
                .queryParam("date", date)
                .queryParam("hl", hl)
                .queryParam("api_key", apiKey)
                .toUriString();

        try {
            log.debug("Calling SerpAPI Related Topics: q={}, geo={}", q, geo);
            return restTemplate.getForObject(url, SerpRelatedTopicsResponse.class);
        } catch (Exception e) {
            log.error("Error calling SerpAPI Related Topics: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to fetch related topics from SerpAPI", e);
        }
    }

    /**
     * Search Baidu
     * 
     * @param query Search query
     */
    public SerpBaiduSearchResponse searchBaidu(String query) {
        String url = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .queryParam("engine", "baidu")
                .queryParam("q", query)
                .queryParam("api_key", apiKey)
                .toUriString();

        try {
            log.debug("Calling SerpAPI Baidu Search: q={}", query);
            return restTemplate.getForObject(url, SerpBaiduSearchResponse.class);
        } catch (Exception e) {
            log.error("Error calling SerpAPI Baidu Search: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to search Baidu via SerpAPI", e);
        }
    }

    /**
     * Get Google Trends Autocomplete suggestions
     * 
     * @param q  Search query to get autocomplete suggestions for
     * @param hl Language code (optional)
     */
    public SerpAutocompleteResponse getAutocomplete(String q, String hl) {
        String url = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .queryParam("engine", "google_trends_autocomplete")
                .queryParam("q", q)
                .queryParam("hl", hl)
                .queryParam("api_key", apiKey)
                .toUriString();

        try {
            log.debug("Calling SerpAPI Autocomplete: q={}", q);
            return restTemplate.getForObject(url, SerpAutocompleteResponse.class);
        } catch (Exception e) {
            log.error("Error calling SerpAPI Autocomplete: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to get autocomplete from SerpAPI", e);
        }
    }
}
