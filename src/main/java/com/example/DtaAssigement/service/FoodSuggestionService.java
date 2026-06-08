package com.example.DtaAssigement.service;

import com.example.DtaAssigement.dto.FoodSuggestionRequest;
import com.example.DtaAssigement.dto.FoodSuggestionResponse;
import com.example.DtaAssigement.entity.FoodSuggestion;
import com.example.DtaAssigement.entity.User;
import com.example.DtaAssigement.repository.FoodSuggestionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * Service for handling food suggestions from users
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class FoodSuggestionService {

    private final FoodSuggestionRepository repository;
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * Submit new food suggestion
     * If food already suggested, increment votes
     * 
     * @param request Suggestion details
     * @param user    User who made suggestion (not null)
     * @return Response with suggestion data
     */
    @Transactional
    public FoodSuggestionResponse submitSuggestion(FoodSuggestionRequest request, User user) {
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Vui lòng đăng nhập để thực hiện hành động này.");
        }

        // Rate limit: 10 times per user per month
        String currentMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        String limitKey = "rate:food-suggest:" + user.getId() + ":" + currentMonth;

        Long count = 0L;
        try {
            if (redisTemplate != null) {
                count = redisTemplate.opsForValue().increment(limitKey);
                if (count == null) {
                    count = 1L;
                }
                if (count == 1) {
                    redisTemplate.expire(limitKey, Duration.ofDays(32));
                }
            } else {
                log.warn("RedisTemplate is not available. Rate limit check is skipped.");
            }
        } catch (Exception e) {
            log.error("Failed to increment rate limit key in Redis, bypassing rate limit check", e);
        }

        if (count > 10) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Bạn đã vượt quá giới hạn đề xuất 10 món ăn trong tháng này.");
        }

        // Normalize food name to lowercase for consistency
        String normalizedFoodName = request.getFoodName().trim().toLowerCase();

        log.info("Receiving food suggestion: {} (normalized: {})", request.getFoodName(), normalizedFoodName);

        // Check if food already suggested (using normalized name)
        Optional<FoodSuggestion> existing = repository.findByFoodNameIgnoreCase(normalizedFoodName);

        if (existing.isPresent()) {
            // Increment votes for existing suggestion
            FoodSuggestion suggestion = existing.get();
            suggestion.setVotes(suggestion.getVotes() + 1);
            repository.save(suggestion);

            log.info("Incremented votes for '{}' to {}", normalizedFoodName, suggestion.getVotes());
            return convertToResponse(suggestion);
        }

        // Create new suggestion with normalized name
        FoodSuggestion suggestion = FoodSuggestion.builder()
                .foodName(normalizedFoodName) // Use normalized lowercase name
                .description(request.getDescription())
                .category(request.getCategory())
                .user(user)
                .userEmail(user.getEmail())
                .build();

        repository.save(suggestion);

        log.info("Created new food suggestion: {} (ID: {})", suggestion.getFoodName(), suggestion.getId());
        return convertToResponse(suggestion);
    }

    /**
     * Get top requested foods by votes
     * 
     * @param limit Maximum number of results
     * @return List of top suggestions
     */
    public List<FoodSuggestionResponse> getTopSuggestions(int limit) {
        log.debug("Getting top {} food suggestions", limit);

        List<FoodSuggestion> suggestions = repository.findTop10ByOrderByVotesDesc();

        return suggestions.stream()
                .limit(limit)
                .map(this::convertToResponse)
                .toList();
    }

    /**
     * Get all suggestions ordered by votes
     * 
     * @return List of all suggestions
     */
    public List<FoodSuggestionResponse> getAllSuggestions() {
        log.debug("Getting all food suggestions");

        return repository.findAllByOrderByVotesDesc().stream()
                .map(this::convertToResponse)
                .toList();
    }

    /**
     * Convert entity to DTO
     */
    private FoodSuggestionResponse convertToResponse(FoodSuggestion suggestion) {
        return FoodSuggestionResponse.builder()
                .id(suggestion.getId())
                .foodName(suggestion.getFoodName())
                .description(suggestion.getDescription())
                .category(suggestion.getCategory())
                .votes(suggestion.getVotes())
                .createdAt(suggestion.getCreatedAt())
                .userEmail(suggestion.getUserEmail())
                .build();
    }
}
