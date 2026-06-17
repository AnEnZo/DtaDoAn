package com.example.DtaAssigement.mapper;

import com.example.DtaAssigement.dto.RestaurantTableDTO;
import com.example.DtaAssigement.entity.RestaurantTable;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@AllArgsConstructor
@Component
public class TableMapper {

    // Dùng khi tạo mới: cần truyền đối tượng Branch đã được lấy từ DB
    public static RestaurantTable toEntity(RestaurantTableDTO dto) {
        RestaurantTable table = new RestaurantTable();
        table.setName(dto.getName());
        // available is optional on input (defaults to true); guard against unboxing null
        table.setAvailable(dto.getAvailable() != null ? dto.getAvailable() : true);
        table.setCapacity(dto.getCapacity() != null ? dto.getCapacity() : 0);
        table.setId(dto.getId());
        return table;
    }

    public static RestaurantTableDTO toDTO(RestaurantTable table) {
        return RestaurantTableDTO.builder()
                .name(table.getName())
                .available(table.isAvailable())
                .capacity(table.getCapacity())
                .id(table.getId())
                .build();
    }
}
