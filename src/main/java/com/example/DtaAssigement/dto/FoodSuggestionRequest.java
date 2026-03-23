package com.example.DtaAssigement.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for food suggestion request from frontend survey
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FoodSuggestionRequest {

    @NotBlank(message = "Tên món ăn không được để trống")
    @Size(min = 2, max = 255, message = "Tên món ăn phải từ 2-255 ký tự")
    private String foodName;

    @Size(max = 1000, message = "Mô tả không quá 1000 ký tự")
    private String description;

    private String category; // Optional: MAIN_DISH, DESSERT, DRINK, APPETIZER, etc.

    // For anonymous submissions
    @Email(message = "Email không hợp lệ")
    private String email;
}
