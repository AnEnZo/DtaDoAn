package com.example.DtaAssigement.repository;

import com.example.DtaAssigement.entity.RawGoogleTrend;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

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

    /**
     * Lấy raw trends phân trang theo (location, ngày) với offset + limit.
     * Order theo fetchedAt DESC (mới nhất trước).
     */
    Page<RawGoogleTrend> findByLocationAndFetchedDateOrderByFetchedAtDesc(
            String location, LocalDate fetchedDate, Pageable pageable);

    /**
     * Lấy tất cả raw trends phân trang theo location, không lọc theo ngày.
     * Order theo fetchedAt DESC (mới nhất trước).
     */
    Page<RawGoogleTrend> findByLocationOrderByFetchedAtDesc(String location, Pageable pageable);

    /**
     * Lấy tất cả raw trends phân trang, không lọc theo location hay ngày.
     * Order theo fetchedAt DESC (mới nhất trước).
     */
    Page<RawGoogleTrend> findAllByOrderByFetchedAtDesc(Pageable pageable);

    /**
     * Đếm tổng số record cho (location, ngày) — dùng để xác nhận totalItems.
     */
    long countByLocationAndFetchedDate(String location, LocalDate fetchedDate);
}
