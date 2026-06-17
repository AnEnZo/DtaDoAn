package com.example.DtaAssigement.service.impl;

import com.example.DtaAssigement.dto.RestaurantTableDTO;
import com.example.DtaAssigement.entity.RestaurantTable;
import com.example.DtaAssigement.mapper.TableMapper;
import com.example.DtaAssigement.repository.TableRepository;
import com.example.DtaAssigement.service.TableService;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
@Transactional
@AllArgsConstructor
public class TableServiceImpl implements TableService {


    private final TableRepository tableRepo;

    @Override
    public List<RestaurantTableDTO> getAllTables() {
        return tableRepo.findByDeletedFalse()
                .stream()
                .map(TableMapper::toDTO)
                .collect(Collectors.toList());
    }


    @Override
    public RestaurantTableDTO createTable(RestaurantTableDTO tableDTO) {
        if (tableDTO.getName() != null) {
            // Kiểm tra nếu bàn (chưa bị xóa) đã tồn tại
            if (tableRepo.existsByNameAndDeletedFalse(tableDTO.getName())) {
                throw new IllegalStateException("Bàn đã tồn tại với tên: " + tableDTO.getName());
            }
        }


        RestaurantTable table = TableMapper.toEntity(tableDTO);

        table.setAvailable(true); // mặc định là true khi tạo mới
        table.setDeleted(false);

        RestaurantTable created = tableRepo.save(table);

        return TableMapper.toDTO(created);
    }

    @Override
    public RestaurantTableDTO updateTable(Long id, RestaurantTableDTO tableDTO) {
        RestaurantTable table = tableRepo.findById(id)
                .filter(t -> !t.isDeleted())
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy bàn với id: " + id));

        // Đổi tên: chặn trùng với bàn khác (chưa xóa)
        if (tableDTO.getName() != null && !tableDTO.getName().equals(table.getName())) {
            if (tableRepo.existsByNameAndDeletedFalse(tableDTO.getName())) {
                throw new IllegalStateException("Bàn đã tồn tại với tên: " + tableDTO.getName());
            }
            table.setName(tableDTO.getName());
        }
        if (tableDTO.getCapacity() != null) {
            table.setCapacity(tableDTO.getCapacity());
        }

        RestaurantTable updated = tableRepo.save(table);
        return TableMapper.toDTO(updated);
    }

    @Override
    public RestaurantTable updateTableStatus(Long id, boolean available) {
        RestaurantTable table = tableRepo.findById(id)
                .filter(t -> !t.isDeleted())
                .orElseThrow(() -> new NoSuchElementException("Table not found with id: " + id));
        table.setAvailable(available);
        return tableRepo.save(table);
    }

    @Override
    public boolean deleteTable(Long id){
        RestaurantTable table = tableRepo.findById(id).orElse(null);
        if (table == null || table.isDeleted()) {
            return false;
        }
        // Không cho xóa bàn đang được sử dụng (có đơn đang phục vụ)
        if (!table.isAvailable()) {
            throw new IllegalStateException("Bàn đang được sử dụng, không thể xóa.");
        }
        // Xóa mềm: giữ lại bản ghi để bảo toàn lịch sử/đơn hàng liên quan
        table.setDeleted(true);
        tableRepo.save(table);
        return true;
    }




    @Override
    public List<RestaurantTableDTO> getListAvailableTables() {
        return tableRepo.findByAvailableTrueAndDeletedFalse()
                .stream()
                .map(TableMapper::toDTO)
                .collect(Collectors.toList());
    }


}
