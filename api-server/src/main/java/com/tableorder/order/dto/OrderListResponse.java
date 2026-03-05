package com.tableorder.order.dto;

import com.tableorder.order.entity.Order;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class OrderListResponse {

    private Long sessionId;
    private List<OrderDto> orders;
    private Integer sessionTotalAmount;

    @Getter
    @Builder
    @AllArgsConstructor
    public static class OrderDto {
        private Long orderId;
        private String orderNumber;
        private String status;
        private Integer totalAmount;
        private List<ItemDto> items;
        private LocalDateTime createdAt;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class ItemDto {
        private String menuName;
        private Integer quantity;
        private Integer unitPrice;
        private Integer subtotal;
    }

    public static OrderListResponse from(Long sessionId, List<Order> orders) {
        List<OrderDto> orderDtos = orders.stream()
                .map(order -> OrderDto.builder()
                        .orderId(order.getId())
                        .orderNumber(order.getOrderNumber())
                        .status(order.getStatus().name())
                        .totalAmount(order.getTotalAmount())
                        .items(order.getItems().stream()
                                .map(item -> ItemDto.builder()
                                        .menuName(item.getMenuName())
                                        .quantity(item.getQuantity())
                                        .unitPrice(item.getUnitPrice())
                                        .subtotal(item.getSubtotal())
                                        .build())
                                .toList())
                        .createdAt(order.getCreatedAt())
                        .build())
                .toList();

        int sessionTotal = orders.stream()
                .mapToInt(Order::getTotalAmount)
                .sum();

        return OrderListResponse.builder()
                .sessionId(sessionId)
                .orders(orderDtos)
                .sessionTotalAmount(sessionTotal)
                .build();
    }
}
