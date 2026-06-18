package com.example.DtaAssigement.repository;

import com.example.DtaAssigement.entity.MenuItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MenuItemRepository extends JpaRepository<MenuItem, Long> {
    List<MenuItem> findByCategoryName(String categoryName);

    List<MenuItem> findByCategoryId(Long categoryId);

    // Soft-delete aware listings: chỉ trả về món chưa bị xóa mềm.
    List<MenuItem> findByDeletedFalse();

    Optional<MenuItem> findByIdAndDeletedFalse(Long id);

    List<MenuItem> findByCategoryNameAndDeletedFalse(String categoryName);

    List<MenuItem> findByCategoryIdAndDeletedFalse(Long categoryId);
}
