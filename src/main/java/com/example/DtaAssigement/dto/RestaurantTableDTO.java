package com.example.DtaAssigement.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class RestaurantTableDTO {
    private Long id;

    @NotBlank(message = "Tên bàn không được để trống")
    private String name;

    // Optional on input: tables are always created as available; status is managed
    // separately (updateTableStatus / order flow). Returned in responses for display.
    private Boolean available;

    @NotNull(message = "Sức chứa là bắt buộc")
    @Min(value = 1, message = "Sức chứa phải lớn hơn hoặc bằng 1")
    private Integer capacity;

}
