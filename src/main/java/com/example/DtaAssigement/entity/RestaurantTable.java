package com.example.DtaAssigement.entity;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class RestaurantTable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Tên bàn không được để trống")
    @Size(max = 50, message = "Tên bàn không được vượt quá 50 ký tự")
    private String name;

    private boolean available;

    private int capacity;

    // Soft-delete flag: deleted tables are hidden from listings but kept for history/FK integrity.
    // columnDefinition adds a DB default so adding this NOT NULL column to existing rows succeeds.
    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean deleted = false;

}
