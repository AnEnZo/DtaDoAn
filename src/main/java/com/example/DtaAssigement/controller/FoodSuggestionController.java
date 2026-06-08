package com.example.DtaAssigement.controller;

import com.example.DtaAssigement.dto.FoodSuggestionRequest;
import com.example.DtaAssigement.dto.FoodSuggestionResponse;
import com.example.DtaAssigement.entity.User;
import com.example.DtaAssigement.security.CustomUserDetails;
import com.example.DtaAssigement.service.FoodSuggestionService;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for Food Suggestion Survey API
 * Allows users to submit suggestions for new menu items
 */
@RestController
@RequestMapping("/api/food-suggestions")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Slf4j
public class FoodSuggestionController {

    private final FoodSuggestionService foodSuggestionService;

    /**
     * Submit food suggestion from user survey
     * If dish already suggested, increments vote count
     * 
     * @param request Food suggestion details
     * @param currentUser Authenticated user details
     * @return Created suggestion with vote count
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<FoodSuggestionResponse> submitSuggestion(
            @Valid @RequestBody FoodSuggestionRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        log.info("POST /api/food-suggestions - Food: {}", request.getFoodName());

        if (currentUser == null) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Vui lòng đăng nhập để thực hiện hành động này.");
        }

        User user = currentUser.getUser();
        if (user == null) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Vui lòng đăng nhập để thực hiện hành động này.");
        }

        FoodSuggestionResponse response = foodSuggestionService.submitSuggestion(request, user);

        log.info("Suggestion submitted: {} (votes: {})", response.getFoodName(), response.getVotes());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get top requested foods by vote count
     * 
     * @param limit Number of results (default: 10, max: 50)
     * @return List of top suggestions
     */
    @GetMapping("/top")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<FoodSuggestionResponse>> getTopSuggestions(
            @RequestParam(defaultValue = "10") int limit) {

        log.info("GET /api/food-suggestions/top?limit={}", limit);

        // Cap limit at 50
        if (limit > 50) {
            log.warn("Limit {} exceeds maximum of 50, using 50", limit);
            limit = 50;
        }

        List<FoodSuggestionResponse> suggestions = foodSuggestionService.getTopSuggestions(limit);

        log.info("Returning {} top suggestions", suggestions.size());
        return ResponseEntity.ok(suggestions);
    }

    /**
     * Get all suggestions ordered by votes
     * 
     * @return List of all suggestions
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<FoodSuggestionResponse>> getAllSuggestions() {

        log.info("GET /api/food-suggestions");

        List<FoodSuggestionResponse> suggestions = foodSuggestionService.getAllSuggestions();

        log.info("Returning {} suggestions", suggestions.size());
        return ResponseEntity.ok(suggestions);
    }
}
