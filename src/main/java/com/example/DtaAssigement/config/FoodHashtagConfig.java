package com.example.DtaAssigement.config;

import com.example.DtaAssigement.entity.FoodSuggestion;
import com.example.DtaAssigement.repository.FoodSuggestionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service cung cấp danh sách hashtag ẩm thực để track trên Instagram.
 * Mỗi lần gọi getHashtagList() sẽ query DB để lấy dữ liệu mới nhất —
 * không cần restart khi có food suggestion mới.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FoodHashtagConfig {

    private final FoodSuggestionRepository foodSuggestionRepository;

    /**
     * Fallback hashtags nếu DB trống hoặc không có suggestion gần đây
     */
    private static final List<String> FALLBACK_HASHTAGS = List.of(
            "pho", // Phở
            "banhmi", // Bánh mì
            "buncha", // Bún chả
            "comtam", // Cơm tấm
            "banhxeo", // Bánh xèo
            "goicuon", // Gỏi cuốn
            "bunbo", // Bún bò
            "cacom", // Cá kho tộ
            "miqua", // Mì quảng
            "banhcuon" // Bánh cuốn
    );

    /**
     * Lấy danh sách hashtag từ top 10 food suggestions trong 14 ngày gần nhất.
     * Gọi DB mỗi lần để phản ánh suggestions mới mà không cần restart server.
     *
     * @return List hashtag đã normalize (không dấu, viết liền, lowercase)
     */
    public List<String> getHashtagList() {
        try {
            LocalDateTime fourteenDaysAgo = LocalDateTime.now().minusDays(14);

            List<FoodSuggestion> recentSuggestions = foodSuggestionRepository
                    .findTop10ByCreatedAtAfterOrderByVotesDesc(fourteenDaysAgo);

            if (!recentSuggestions.isEmpty()) {
                List<String> hashtags = recentSuggestions.stream()
                        .map(FoodSuggestion::getFoodName)
                        .map(this::normalizeToHashtag)
                        .collect(Collectors.toList());

                log.info("Loaded {} hashtags from DB food suggestions (last 14 days): {}", hashtags.size(), hashtags);
                return hashtags;
            }

            log.warn("No food suggestions in last 14 days, using fallback hashtags");
            return FALLBACK_HASHTAGS;

        } catch (Exception e) {
            log.error("Error loading food hashtags from DB: {}", e.getMessage());
            return FALLBACK_HASHTAGS;
        }
    }

    /**
     * Normalize tên món ăn tiếng Việt → Instagram hashtag format
     * Ví dụ: "Bún Riêu Cua" → "bunrieucua"
     */
    public String normalizeToHashtag(String foodName) {
        if (foodName == null || foodName.isEmpty())
            return "";

        String normalized = Normalizer.normalize(foodName, Normalizer.Form.NFD);
        String withoutDiacritics = normalized.replaceAll("\\p{M}", "");
        withoutDiacritics = withoutDiacritics.replace("đ", "d").replace("Đ", "d");

        String hashtag = withoutDiacritics
                .replaceAll("\\s+", "")
                .replaceAll("[^a-zA-Z0-9]", "")
                .toLowerCase();

        log.debug("Normalized '{}' → '{}'", foodName, hashtag);
        return hashtag;
    }
}
