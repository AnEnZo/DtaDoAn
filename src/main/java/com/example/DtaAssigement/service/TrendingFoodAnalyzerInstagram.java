package com.example.DtaAssigement.service;

import com.example.DtaAssigement.config.FoodHashtagConfig;
import com.example.DtaAssigement.dto.InstagramPostDTO;
import com.example.DtaAssigement.entity.TrendingFood;
import com.example.DtaAssigement.repository.TrendingFoodRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service for analyzing Instagram trending foods.
 * Uses Instagram API to calculate trending scores based on engagement and
 * growth.
 * Results are persisted to DB (upsert) for historical tracking.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TrendingFoodAnalyzerInstagram {

    private final InstagramService instagramService;
    private final FoodHashtagConfig foodHashtagConfig; // dynamic — query DB mỗi lần
    private final TrendingFoodRepository trendingFoodRepository;

    /**
     * Analyze all food hashtags and calculate trending scores.
     * Hashtag list is fetched fresh from DB each time (no restart needed).
     * Results are upserted into trending_foods table.
     *
     * @param topN Number of top trending foods to return
     * @return List of trending foods sorted by instagramScore DESC
     */
    @Transactional
    public List<TrendingFood> analyzeHotTrending(int topN) {
        List<String> hashtagList = foodHashtagConfig.getHashtagList(); // fresh from DB
        log.info("🔍 Starting hot trending analysis for {} hashtags: {}", hashtagList.size(), hashtagList);

        List<TrendingFood> results = new ArrayList<>();

        for (String hashtag : hashtagList) {
            try {
                TrendingFood food = analyzeSingleHashtag(hashtag);
                if (food != null && food.getInstagramScore() != null && food.getInstagramScore() > 0) {
                    TrendingFood saved = saveOrUpdate(food); // 💾 persist
                    results.add(saved);
                    log.debug("✓ Saved #{}: score={}", hashtag, saved.getInstagramScore());
                }
            } catch (Exception e) {
                log.error("❌ Error analyzing hashtag #{}: {}", hashtag, e.getMessage());
            }
        }

        // Sort by instagram score descending
        results.sort((a, b) -> Double.compare(b.getInstagramScore(), a.getInstagramScore()));

        // Assign ranks (trendScore = rank)
        for (int i = 0; i < results.size(); i++) {
            results.get(i).setTrendScore(i + 1);
        }
        // Save updated ranks
        trendingFoodRepository.saveAll(results);

        log.info("✅ Analysis complete. {} trending foods found & saved.", results.size());

        return results.stream().limit(topN).collect(Collectors.toList());
    }

    /**
     * Upsert TrendingFood vào DB:
     * - Nếu đã có (query, location="INSTAGRAM") → UPDATE fields Instagram
     * - Nếu chưa có → INSERT mới
     */
    private TrendingFood saveOrUpdate(TrendingFood food) {
        Optional<TrendingFood> existing = trendingFoodRepository
                .findByQueryAndLocation(food.getQuery(), "INSTAGRAM");

        if (existing.isPresent()) {
            TrendingFood entity = existing.get();
            entity.setInstagramHashtagId(food.getInstagramHashtagId());
            entity.setAvgEngagement(food.getAvgEngagement());
            entity.setRecentEngagement(food.getRecentEngagement());
            entity.setGrowthRate(food.getGrowthRate());
            entity.setInstagramScore(food.getInstagramScore());
            entity.setTotalPosts24h(food.getTotalPosts24h());
            entity.setStatus(food.getStatus());
            entity.setLastUpdatedAt(LocalDateTime.now());
            log.debug("↻ Updated existing Instagram trend: #{}", food.getQuery());
            return trendingFoodRepository.save(entity);
        } else {
            food.setLocation("INSTAGRAM");
            food.setDataSource("INSTAGRAM");
            food.setFirstSeenAt(LocalDateTime.now());
            food.setLastUpdatedAt(LocalDateTime.now());
            log.debug("+ Inserted new Instagram trend: #{}", food.getQuery());
            return trendingFoodRepository.save(food);
        }
    }

    /**
     * Analyze single hashtag to calculate trending data
     */
    private TrendingFood analyzeSingleHashtag(String hashtag) {
        log.debug("Analyzing hashtag: #{}", hashtag);

        String hashtagId = instagramService.searchHashtag(hashtag);
        if (hashtagId == null) {
            log.warn("Hashtag not found: #{}", hashtag);
            return null;
        }

        List<InstagramPostDTO> topPosts = instagramService.getTopMedia(hashtagId, 15);
        if (topPosts.isEmpty()) {
            log.warn("No top posts found for #{}", hashtag);
            return null;
        }

        double avgEngagement = calculateEngagement(topPosts);

        List<InstagramPostDTO> recentPosts = instagramService.getRecentMedia(hashtagId, 15);
        if (recentPosts.isEmpty()) {
            log.warn("No recent posts found for #{}", hashtag);
        }

        double recentEngagement = recentPosts.isEmpty() ? avgEngagement : calculateEngagement(recentPosts);
        double growthRate = avgEngagement > 0 ? recentEngagement / avgEngagement : 1.0;

        TrendingFood food = TrendingFood.builder()
                .query(hashtag)
                .instagramHashtagId(hashtagId)
                .avgEngagement(avgEngagement)
                .recentEngagement(recentEngagement)
                .growthRate(growthRate)
                .totalPosts24h(recentPosts.size())
                .dataSource("INSTAGRAM")
                .location("INSTAGRAM")
                .status(determineTrendStatus(growthRate))
                .build();

        food.calculateInstagramScore();

        log.debug("#{}: avgEng={}, growth={}, score={}, status={}",
                hashtag,
                String.format("%.1f", avgEngagement),
                String.format("%.2f", growthRate),
                String.format("%.1f", food.getInstagramScore()),
                food.getStatus());

        return food;
    }

    /**
     * Formula: Average of (likes + comments × 2) across all posts
     */
    private double calculateEngagement(List<InstagramPostDTO> posts) {
        if (posts.isEmpty())
            return 0.0;

        double totalEngagement = posts.stream()
                .mapToDouble(p -> {
                    int likes = p.getLikeCount() != null ? p.getLikeCount() : 0;
                    int comments = p.getCommentsCount() != null ? p.getCommentsCount() : 0;
                    return likes + (comments * 2.0);
                })
                .sum();

        return totalEngagement / posts.size();
    }

    /**
     * Determine trend status based on growth rate
     */
    private String determineTrendStatus(double growthRate) {
        if (growthRate >= 1.5)
            return "VIRAL";
        if (growthRate >= 1.2)
            return "TRENDING";
        if (growthRate >= 0.8)
            return "STABLE";
        return "DECLINING";
    }
}
