package com.example.DtaAssigement.service;

import com.example.DtaAssigement.dto.FacebookPostDTO;
import com.restfb.Connection;
import com.restfb.FacebookClient;
import com.restfb.Parameter;
import com.restfb.exception.FacebookException;
import com.restfb.types.Post;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for Facebook Graph API operations
 */
@Service
@Slf4j
public class FacebookService {

    @Autowired(required = false)
    private FacebookClient facebookClient;

    /**
     * Get posts from a Facebook Page
     * 
     * @param pageId Facebook Page ID
     * @param limit  Number of posts to fetch
     * @return List of Facebook posts
     */
    public List<FacebookPostDTO> getPagePosts(String pageId, int limit) {
        if (facebookClient == null) {
            log.error("Facebook client is not configured. Cannot fetch posts.");
            return List.of();
        }

        try {
            log.debug("Fetching {} posts from Facebook page: {}", limit, pageId);

            Connection<Post> posts = facebookClient.fetchConnection(
                    pageId + "/posts",
                    Post.class,
                    Parameter.with("fields",
                            "id,message,created_time,reactions.summary(true),shares,comments.summary(true)"),
                    Parameter.with("limit", limit));

            List<FacebookPostDTO> result = posts.getData().stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());

            log.info("Successfully fetched {} posts from Facebook page: {}", result.size(), pageId);
            return result;

        } catch (FacebookException e) {
            log.error("Facebook API error while fetching posts from page {}: {}", pageId, e.getMessage());
            return List.of();
        } catch (Exception e) {
            log.error("Unexpected error while fetching Facebook posts: {}", e.getMessage(), e);
            return List.of();
        }
    }

    /**
     * Convert RestFB Post object to DTO
     */
    private FacebookPostDTO convertToDTO(Post post) {
        return FacebookPostDTO.builder()
                .id(post.getId())
                .message(post.getMessage())
                .createdTime(post.getCreatedTime() != null ? post.getCreatedTime().toString() : null)
                .reactions(post.getReactionsCount() != null ? post.getReactionsCount().intValue() : 0)
                .shares(post.getSharesCount() != null ? post.getSharesCount().intValue() : 0)
                .comments(0) // Summary doesn't provide count easily, set to 0 for now
                .build();
    }
}
