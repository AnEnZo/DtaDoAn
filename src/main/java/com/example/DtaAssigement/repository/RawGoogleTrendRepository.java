package com.example.DtaAssigement.repository;

import com.example.DtaAssigement.entity.RawGoogleTrend;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository for RawGoogleTrend entity.
 * Lưu data thô từ SerpAPI/Google Trends trước khi Llama phân tích.
 */
@Repository
public interface RawGoogleTrendRepository extends JpaRepository<RawGoogleTrend, Long> {

    /**
     * Kiểm tra đã có data cho (location, ngày hôm nay) chưa.
     * Dùng cho dedup check ở API 1 (table-level).
     */
    boolean existsByLocationAndFetchedDate(String location, LocalDate fetchedDate);

    /**
     * Lấy tất cả raw trends của (location, ngày) — dùng ở API 2 để gửi sang Llama.
     */
    List<RawGoogleTrend> findByLocationAndFetchedDate(String location, LocalDate fetchedDate);

    /**
     * Tìm một item cụ thể theo query+location — dùng khi join sau Llama.
     */
    Optional<RawGoogleTrend> findByQueryAndLocationAndFetchedDate(
            String query, String location, LocalDate fetchedDate);
}
