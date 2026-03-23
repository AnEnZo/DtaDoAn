package com.example.DtaAssigement.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entity representing food suggestions from users
 * Tracks what dishes users want the restaurant to add to menu
 */
@Entity
@Table(name = "food_suggestions", indexes = {
        @Index(name = "idx_food_name", columnList = "food_name"),
        @Index(name = "idx_created_at", columnList = "created_at DESC")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FoodSuggestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "food_name", nullable = false, length = 255)
    private String foodName; // Tên món ăn được đề xuất

    @Column(name = "description", columnDefinition = "TEXT")
    private String description; // Mô tả thêm về món ăn

    @Column(name = "category", length = 100)
    private String category; // Phân loại: MAIN_DISH, DESSERT, DRINK, etc.

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user; // Người đề xuất (optional nếu cho phép anonymous)

    @Column(name = "user_email", length = 255)
    private String userEmail; // Email người đề xuất (cho anonymous)

    @Column(name = "votes")
    @Builder.Default
    private Integer votes = 1; // Số lượt vote/request cho món này

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
