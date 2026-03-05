package com.tableorder.order.dto;

import com.tableorder.order.entity.Order;
import com.tableorder.order.entity.OrderItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class CreateOrderResponse {

    private Long orderId;
    private String orderNumber;
    private String status;
    private Integer totalAmount;
    private List<OrderItemDto> items;
    private LocalDateTime createdAt;

    @Getter
    @Builder
    @AllArgsConstructor
    public static class OrderItemDto {
        private Long orderItemId;
        private Long menuId;
        private String menuName;
        private Integer quantity;
        private Integer unitPrice;
        private Integer subtotal;
    }

    public static CreateOrderResponse from(Order order) {
        List<OrderItemDto> itemDtos = order.getItems().stream()
                .map(item -> OrderItemDto.builder()
                        .orderItemId(item.getId())
                        .menuId(item.getMenuId())
                        .menuName(item.getMenuName())
                        .quantity(item.getQuantity())
                        .unitPrice(item.getUnitPrice())
                        .subtotal(item.getSubtotal())
                        .build())
                .toList();

        return CreateOrderResponse.builder()
                .orderId(order.getId())
                .orderNumber(order.getOrderNumber())
                .status(order.getStatus().name())
                .totalAmount(order.getTotalAmount())
                .items(itemDtos)
                .createdAt(order.getCreatedAt())
                .build();
    }
}
