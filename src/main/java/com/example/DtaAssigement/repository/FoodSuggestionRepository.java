package com.example.DtaAssigement.repository;

import com.example.DtaAssigement.entity.FoodSuggestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for FoodSuggestion entity
 */
@Repository
public interface FoodSuggestionRepository extends JpaRepository<FoodSuggestion, Long> {

    /**
     * Find by food name (case-insensitive)
     */
    Optional<FoodSuggestion> findByFoodNameIgnoreCase(String foodName);

    /**
     * Get all suggestions ordered by votes descending
     */
    List<FoodSuggestion> findAllByOrderByVotesDesc();

    /**
     * Get top N suggestions by votes
     */
    List<FoodSuggestion> findTop10ByOrderByVotesDesc();

    /**
     * Get top suggestions from last N days, ordered by votes
     * 
     * @param since Date threshold (e.g., 14 days ago)
     * @return Top 10 suggestions created after the given date
     */
    List<FoodSuggestion> findTop10ByCreatedAtAfterOrderByVotesDesc(LocalDateTime since);

    /**
     * Count suggestions by food name
     */
    Long countByFoodNameIgnoreCase(String foodName);
}
