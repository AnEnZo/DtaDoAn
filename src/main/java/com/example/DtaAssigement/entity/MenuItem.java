package com.example.DtaAssigement.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MenuItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(hidden = true)
    private Long id;

    @NotBlank(message = "Tên món không được để trống")
    @Size(max = 50, message = "Tên món không được vượt quá 50 ký tự")
    private String name;

    @Positive(message = "Giá món phải lớn hơn 0")
    @Column(precision = 12, scale = 2)
    private BigDecimal price;

    @NotBlank(message = "Ảnh món ăn không được để trống")
    @Column(name = "image_url", columnDefinition = "TEXT")
    private String imageUrl;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;

    // Soft-delete flag: deleted menu items are hidden from listings but kept so
    // older orders/invoices still display their items (history + FK integrity).
    // columnDefinition adds a DB default so adding this NOT NULL column to existing rows succeeds.
    @Builder.Default
    @Column(nullable = false, columnDefinition = "boolean default false")
    @Schema(hidden = true)
    private boolean deleted = false;

    @OneToMany(mappedBy = "menuItem", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonBackReference
    private List<OrderItem> orderItems;

}
