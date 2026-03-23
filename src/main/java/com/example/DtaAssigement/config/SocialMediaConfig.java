package com.example.DtaAssigement.config;

import com.restfb.DefaultFacebookClient;
import com.restfb.FacebookClient;
import com.restfb.Version;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Configuration for Facebook, Instagram, and YouTube API integration
 */
@Configuration
@Slf4j
public class SocialMediaConfig {

    @Value("${facebook.access-token:}")
    private String facebookAccessToken;

    @Value("${instagram.access-token:}")
    private String instagramAccessToken;

    @Value("${instagram.business-account-id:}")
    private String instagramBusinessAccountId;

    @Value("${youtube.api-key:}")
    private String youtubeApiKey;

    @Bean
    public FacebookClient facebookClient() {
        if (facebookAccessToken == null || facebookAccessToken.isEmpty()) {
            log.warn("Facebook access token is not configured. Facebook API will not work.");
            return null;
        }

        log.info("Initializing Facebook client with API version: {}", Version.LATEST);
        return new DefaultFacebookClient(
                facebookAccessToken,
                Version.LATEST);
    }

    @Bean
    @Qualifier("socialMediaRestTemplate")
    public RestTemplate socialMediaRestTemplate() {
        return new RestTemplate();
    }

    // Getters for Instagram credentials
    public String getInstagramAccessToken() {
        return instagramAccessToken;
    }

    public String getInstagramBusinessAccountId() {
        return instagramBusinessAccountId;
    }

    // Getter for YouTube API key
    public String getYoutubeApiKey() {
        return youtubeApiKey;
    }
}
