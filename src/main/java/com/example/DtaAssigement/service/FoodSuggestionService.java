package com.example.DtaAssigement.service;

import com.example.DtaAssigement.dto.FoodSuggestionRequest;
import com.example.DtaAssigement.dto.FoodSuggestionResponse;
import com.example.DtaAssigement.entity.FoodSuggestion;
import com.example.DtaAssigement.entity.User;
import com.example.DtaAssigement.repository.FoodSuggestionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    /**
     * Submit new food suggestion
     * If food already suggested, increment votes
     * 
     * @param request Suggestion details
     * @param user    User who made suggestion (null if anonymous)
     * @return Response with suggestion data
     */
    @Transactional
    public FoodSuggestionResponse submitSuggestion(FoodSuggestionRequest request, User user) {
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
                .userEmail(user != null ? user.getEmail() : request.getEmail())
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
