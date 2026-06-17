package com.example.DtaAssigement.repository;

import com.example.DtaAssigement.entity.RestaurantTable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TableRepository extends JpaRepository<RestaurantTable,Long> {
    RestaurantTable findByName(String name);
    long count(); // Đếm tất cả bàn
    long countByAvailableTrue(); // Đếm số bàn còn trống
    List<RestaurantTable> findByAvailableTrue(); // Lấy danh sách bàn còn trống
    boolean existsByName(String name);

    // Soft-delete aware queries (exclude tables marked deleted)
    List<RestaurantTable> findByDeletedFalse();
    List<RestaurantTable> findByAvailableTrueAndDeletedFalse();
    boolean existsByNameAndDeletedFalse(String name);
    long countByDeletedFalse();
    long countByAvailableTrueAndDeletedFalse();
}
