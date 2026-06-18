package com.example.DtaAssigement.service.impl;

import com.example.DtaAssigement.dto.MenuItemDTO;
import com.example.DtaAssigement.ennum.OrderStatus;
import com.example.DtaAssigement.entity.*;
import com.example.DtaAssigement.mapper.MenuItemMapper;
import com.example.DtaAssigement.repository.*;
import com.example.DtaAssigement.service.MenuItemService;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@Service
@AllArgsConstructor
@Transactional
public class MenuItemServiceImpl implements MenuItemService {

    private final MenuItemRepository menuItemRepo;
    private final CategoryRepository categoryrepository;
    private final CategoryRepository categoryRepo;
    private final OrderItemRepository orderItemRepo;

    @Override
    public List<MenuItem> getAllMenuItems() {
        // Chỉ trả về món chưa bị xóa mềm (món đã xóa vẫn còn trong DB để giữ lịch sử đơn hàng).
        return menuItemRepo.findByDeletedFalse();
    }

    @Override
    public Optional<MenuItem> getMenuItemById(Long id) {
        return menuItemRepo.findByIdAndDeletedFalse(id);
    }

    @Override
    public List<MenuItem> getMenuItemsByCategory(String categoryName) {
        // Có thể kiểm tra tồn tại Category nếu muốn:
        if (!categoryRepo.existsByName(categoryName)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Category '" + categoryName + "' not found");
        }
        return menuItemRepo.findByCategoryNameAndDeletedFalse(categoryName);
    }

    @Override
    public List<MenuItem> getMenuItemsByCategoryId(Long categoryId) {
        if (!categoryRepo.existsById(categoryId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Category with ID " + categoryId + " not found");
        }
        return menuItemRepo.findByCategoryIdAndDeletedFalse(categoryId);
    }

    @Override
    public MenuItemDTO createMenuItem(MenuItemDTO menuItemDTO) {
        // Kiểm tra và lấy tên danh mục
        String categoryName = menuItemDTO.getCategory().getName();
        if (categoryName == null || categoryName.trim().isEmpty()) {
            throw new IllegalArgumentException("Tên danh mục không hợp lệ (null hoặc rỗng).");
        }
        // Tìm danh mục theo tên và id
        Category category = categoryrepository.findByName(categoryName)
                .orElseThrow(
                        () -> new IllegalArgumentException("Danh mục với tên '" + categoryName + "' không tồn tại."));
        // Chuyển DTO thành entity
        MenuItem menuItem = MenuItemMapper.toEntity(menuItemDTO, category);
        menuItem.setCategory(category); // Gán lại để đảm bảo đúng entity
        // Lưu món ăn vào database
        MenuItem created = menuItemRepo.save(menuItem);
        // Chuyển thành DTO để trả về
        return MenuItemMapper.toDTO(created);
    }

    @Override
    public MenuItemDTO updateMenuItem(Long id, MenuItemDTO menuItemDTO) {
        MenuItem existing = menuItemRepo.findById(id)
                .orElseThrow(
                        () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "MenuItem not found with id " + id));

        if (menuItemDTO.getCategory() != null) {
            Category category = categoryRepo.findByName(menuItemDTO.getCategory().getName())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));
            existing.setCategory(category);
            existing.setName(menuItemDTO.getName());
            existing.setPrice(menuItemDTO.getPrice());
            existing.setImageUrl(menuItemDTO.getImageUrl());
            existing.setDescription(menuItemDTO.getDescription());
        } else {
            throw new IllegalArgumentException("Category k dc để trống");
        }

        return MenuItemMapper.toDTO(menuItemRepo.save(existing));
    }

    @Override
    public void deleteMenuItem(Long id) {
        MenuItem item = menuItemRepo.findById(id)
                .orElseThrow(
                        () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "MenuItem not found with id " + id));

        // Nếu món đang nằm trong đơn hàng chưa hoàn tất (chờ phục vụ / đã phục vụ)
        // thì không cho xóa, để tránh phá vỡ đơn đang xử lý.
        boolean inActiveOrder = orderItemRepo.existsByMenuItemIdAndOrderStatusIn(
                id, List.of(OrderStatus.PENDING, OrderStatus.SERVED));
        if (inActiveOrder) {
            throw new IllegalStateException(
                    "Không thể xóa món vì món đang nằm trong đơn hàng chưa thanh toán (đang chờ phục vụ hoặc đã phục vụ).");
        }

        // Xóa mềm: giữ lại bản ghi để các đơn hàng/hóa đơn cũ vẫn hiển thị được món này.
        item.setDeleted(true);
        menuItemRepo.save(item);
    }

}
