package com.example.DtaAssigement.repository;

import com.example.DtaAssigement.entity.TrendingFood;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for TrendingFood entity
 */
@Repository
public interface TrendingFoodRepository extends JpaRepository<TrendingFood, Long> {

        /**
         * Find by query and location
         */
        Optional<TrendingFood> findByQueryAndLocation(String query, String location);

        /**
         * Find all by status, ordered by trend score
         */
        List<TrendingFood> findByStatusOrderByTrendScoreDesc(String status);

        /**
         * Find top trending foods with minimum score
         */
        @Query("SELECT t FROM TrendingFood t WHERE t.status = 'ACTIVE' AND t.trendScore >= :minScore ORDER BY t.trendScore DESC")
        List<TrendingFood> findTopTrendingFoods(@Param("minScore") int minScore);

        /**
         * Find trending foods updated after a certain time
         */
        List<TrendingFood> findByLastUpdatedAtAfterOrderByTrendScoreDesc(LocalDateTime after);

        /**
         * Find by location
         */
        List<TrendingFood> findByLocationOrderByTrendScoreDesc(String location);

        /**
         * Count active trending foods
         */
        long countByStatus(String status);

        /**
         * Find declining trends (for cleanup/archiving)
         */
        @Query("SELECT t FROM TrendingFood t WHERE t.status = 'ACTIVE' AND t.lastUpdatedAt < :threshold ORDER BY t.lastUpdatedAt ASC")
        List<TrendingFood> findStaleActiveTrends(@Param("threshold") LocalDateTime threshold);

        /**
         * Find by location and updated after a certain time, ordered by score
         */
        List<TrendingFood> findByLocationAndLastUpdatedAtAfterOrderByTrendScoreDesc(
                        String location, LocalDateTime after);

        /**
         * Find by location and updated between dates, ordered by score
         */
        List<TrendingFood> findByLocationAndLastUpdatedAtBetweenOrderByTrendScoreDesc(
                        String location, LocalDateTime start, LocalDateTime end);

        /**
         * Per-record dedup check trước khi INSERT vào trending_foods.
         * Kiểm tra (query, location) đã được lưu trong ngày hôm nay chưa.
         */
        @Query("SELECT COUNT(t) > 0 FROM TrendingFood t " +
                        "WHERE t.query = :query AND t.location = :location " +
                        "AND FUNCTION('DATE', t.lastUpdatedAt) = :date")
        boolean existsByQueryAndLocationAndDate(
                        @Param("query") String query,
                        @Param("location") String location,
                        @Param("date") LocalDate date);

        // ===== Paginated get-all methods (offset pagination) =====

        /**
         * Get all trending foods paginated, ordered by lastUpdatedAt DESC (newest first).
         */
        org.springframework.data.domain.Page<TrendingFood> findAllByOrderByLastUpdatedAtDesc(
                        org.springframework.data.domain.Pageable pageable);

        /**
         * Get trending foods by location, paginated, ordered by lastUpdatedAt DESC.
         */
        org.springframework.data.domain.Page<TrendingFood> findByLocationOrderByLastUpdatedAtDesc(
                        String location, org.springframework.data.domain.Pageable pageable);
}
