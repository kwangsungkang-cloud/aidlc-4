package com.tableorder.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class CreateOrderRequest {

    @NotEmpty(message = "주문 항목이 비어있습니다")
    @Valid
    private List<OrderItemRequest> items;

    @Getter
    @NoArgsConstructor
    public static class OrderItemRequest {

        @NotNull(message = "메뉴 ID는 필수입니다")
        private Long menuId;

        @NotNull(message = "수량은 필수입니다")
        @Min(value = 1, message = "수량은 1 이상이어야 합니다")
        private Integer quantity;
    }
}
