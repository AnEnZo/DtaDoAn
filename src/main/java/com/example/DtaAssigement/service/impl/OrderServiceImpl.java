package com.example.DtaAssigement.service.impl;

import com.example.DtaAssigement.aop.audit.Auditable;
import com.example.DtaAssigement.ennum.OrderStatus;
import com.example.DtaAssigement.ennum.OrderType;
import com.example.DtaAssigement.entity.*;
import com.example.DtaAssigement.repository.*;
import com.example.DtaAssigement.security.JwtTokenUtil;
import com.example.DtaAssigement.service.OrderService;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@Transactional
@AllArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepo;
    private final TableRepository tableRepo;
    private final MenuItemRepository menuItemRepo;
    private final OrderItemRepository orderItemRepo;
    private final UserRepository userRepo;
    private final JwtTokenUtil jwtTokenUtil;

    @Override
    public Order getOrderById(Long id) {
        return orderRepo.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Order not found: " + id));
    }

    @Override
    @Auditable(action = "CREATE_ORDER", entityType = "ORDER", description = "Create new dine-in order")
    public Order createOrder(Long tableId) {
        RestaurantTable table = tableRepo.findById(tableId)
                .orElseThrow(() -> new NoSuchElementException("Table not found: " + tableId));

        if (!table.isAvailable()) {
            throw new IllegalStateException("Table " + tableId + " đã có người !");
        }

        String nameStaff = jwtTokenUtil.getCurrentUsername();
        User staff = userRepo.findByUsername(nameStaff)
                .orElseThrow(() -> new IllegalStateException("Staff not found: " + nameStaff));

        Order order = Order.builder()
                .table(table)
                .orderTime(LocalDateTime.now())
                .staff(staff)
                .status(OrderStatus.PENDING)
                .orderType(OrderType.DINE_IN)
                .build();
        table.setAvailable(false);
        tableRepo.save(table);
        return orderRepo.save(order);
    }

    @Override
    public Order createTakeawayOrder() {
        // Lấy thông tin nhân viên hiện tại
        String username = jwtTokenUtil.getCurrentUsername();
        User staff = userRepo.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("Staff not found: " + username));

        Order order = Order.builder()
                .orderTime(LocalDateTime.now())
                .status(OrderStatus.PENDING)
                .orderType(OrderType.TAKEAWAY)
                .staff(staff)
                .build();

        return orderRepo.save(order);
    }

    @Override
    public OrderItem addItemToOrder(Long orderId, Long menuItemId, int quantity) {
        Order order = orderRepo.findById(orderId)
                .orElseThrow(() -> new NoSuchElementException("Order not found: " + orderId));
        MenuItem item = menuItemRepo.findById(menuItemId)
                .orElseThrow(() -> new NoSuchElementException("MenuItem not found: " + menuItemId));

        if (item.isDeleted()) {
            throw new IllegalStateException("Món ăn này đã ngừng kinh doanh và không thể thêm vào đơn hàng.");
        }
        if (item.getCategory() == null) {
            throw new IllegalStateException("Thiếu thông tin chi nhánh hoặc danh mục món ăn.");
        }
        if (order.getStatus() == OrderStatus.PAID) {
            throw new IllegalStateException("Đơn hàng đã thanh toán không thể sửa món");
        }

        // Tìm xem đã có OrderItem cho menuItem này chưa
        OrderItem existing = order.getOrderItems().stream()
                .filter(oi -> oi.getMenuItem().getId().equals(menuItemId))
                .findFirst()
                .orElse(null);

        if (existing != null) {
            // Nếu đã tồn tại, chỉ cần tăng số lượng
            existing.setQuantity(existing.getQuantity() + quantity);
            return orderItemRepo.save(existing);
        } else {
            // Nếu chưa có, tạo mới và thêm vào order
            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .menuItem(item)
                    .quantity(quantity)
                    .build();
            order.getOrderItems().add(orderItem);
            return orderItemRepo.save(orderItem);
        }
    }

    @Override
    public List<Order> getOrdersByStatus(OrderStatus status) {
        return orderRepo.findAll().stream()
                .filter(o -> o.getStatus() == status)
                .toList();
    }

    @Override
    public Page<Order> getAllOrders(Pageable pageable) {
        return orderRepo.findAll(pageable);
    }

    @Override
    public Page<Order> getOrdersByDateRange(LocalDate start, LocalDate end, Pageable pageable) {
        LocalDateTime startDt = start.atStartOfDay();
        LocalDateTime endDt = end.plusDays(1).atStartOfDay().minusNanos(1);
        return orderRepo.findByOrderTimeBetween(startDt, endDt, pageable);
    }

    @Override
    public long countOrdersToday() {
        LocalDate today = LocalDate.now();
        LocalDateTime start = today.atStartOfDay();
        LocalDateTime end = today.plusDays(1).atStartOfDay().minusNanos(1);
        return orderRepo.countByOrderTimeBetween(start, end);
    }

    @Override
    public long countPendingOrders() {
        return orderRepo.countByStatus(OrderStatus.PENDING);
    }

    @Auditable(action = "DELETE_ORDER", entityType = "ORDER", description = "Delete order")
    public boolean deleteOrder(Long id) {
        if (!orderRepo.existsById(id)) {
            return false;
        }
        Order order = orderRepo.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Order not found: " + id));

        // Không cho phép xóa đơn hàng đã thanh toán
        if (order.getStatus() == OrderStatus.PAID) {
            throw new IllegalStateException("Không thể xóa đơn hàng đã thanh toán (PAID).");
        }

        // Chỉ cập nhật bàn nếu đây là DINE_IN
        if (order.getTable() != null) {
            RestaurantTable table = order.getTable();
            table.setAvailable(true);
            tableRepo.save(table);
        }
        orderRepo.deleteById(id);
        return true;
    }

    @Override
    @Auditable(action = "UPDATE_ORDER_STATUS", entityType = "ORDER", description = "Update order status")
    public Order updateOrderStatus(Long orderId, OrderStatus status) {
        Order order = orderRepo.findById(orderId)
                .orElseThrow(() -> new NoSuchElementException("Order not found: " + orderId));
        // Guard valid transitions: PENDING -> SERVED -> PAID
        if (status == OrderStatus.SERVED) {
            if (order.getStatus() != OrderStatus.PENDING) {
                throw new IllegalStateException("Only PENDING orders can be served");
            }
        }
        if (status == OrderStatus.PAID) {
            if (order.getStatus() != OrderStatus.SERVED) {
                throw new IllegalStateException("Only SERVED orders can be paid");
            }
        }
        order.setStatus(status);
        if (status == OrderStatus.PAID) {
            RestaurantTable table = order.getTable();
            table.setAvailable(true);
            tableRepo.save(table);
        }
        return orderRepo.save(order);
    }

    @Override
    public OrderItem removeItemFromOrder(Long orderId, Long menuItemId, int quantityToRemove) {
        if (quantityToRemove <= 0) {
            throw new IllegalArgumentException("Số lượng cần xóa phải lớn hơn 0");
        }
        Order order = orderRepo.findById(orderId)
                .orElseThrow(() -> new NoSuchElementException("Order not found: " + orderId));

        // OrderItem orderItem = order.getOrderItems().stream()
        // .filter(item -> item.getMenuItem().getId().equals(menuItemId))
        // .findFirst()
        // .orElseThrow(() -> new NoSuchElementException("Item not found in order"));

        if (order.getStatus() == OrderStatus.PAID) {
            throw new IllegalStateException("Đơn hàng đã thanh toán không thể sửa món");
        }

        OrderItem orderItem = orderItemRepo.findByOrderIdAndMenuItemId(orderId, menuItemId)
                .orElseThrow(() -> new NoSuchElementException("Item not found in order"));

        int currentQuantity = orderItem.getQuantity();
        if (quantityToRemove > currentQuantity) {
            throw new IllegalArgumentException("Số lượng cần xóa vượt quá số lượng hiện tại trong đơn hàng");
        }

        if (quantityToRemove == currentQuantity) {
            orderItemRepo.delete(orderItem);
            return null; // hoặc throw custom response
        } else {
            orderItem.setQuantity(currentQuantity - quantityToRemove);
            return orderItemRepo.save(orderItem);
        }
    }

    @Override
    public Order getLatestOrderByTableId(Long tableId) {
        return orderRepo.findTopByTableIdOrderByOrderTimeDesc(tableId)
                .orElseThrow(() -> new NoSuchElementException("No order found for table " + tableId));
    }

}
