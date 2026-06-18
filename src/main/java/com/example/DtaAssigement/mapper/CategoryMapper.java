package com.example.DtaAssigement.mapper;

import com.example.DtaAssigement.dto.CategoryDTO;
import com.example.DtaAssigement.entity.Category;
import org.springframework.stereotype.Component;

@Component
public class CategoryMapper {

    public static CategoryDTO toDTO(Category entity) {
        // Món ăn có thể không còn danh mục (danh mục đã bị xóa -> category = null).
        if (entity == null) {
            return null;
        }
        return CategoryDTO.builder()
                .id(entity.getId())
                .name(entity.getName())
                .build();
    }

    public static Category toEntity(CategoryDTO dto) {
        Category category = Category.builder()
                .id(dto.getId())
                .name(dto.getName())
                .build();
        return category;
    }
}
