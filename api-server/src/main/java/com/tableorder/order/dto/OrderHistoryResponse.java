package com.tableorder.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class OrderHistoryResponse {

    private Long tableId;
    private Integer tableNumber;
    private List<SessionHistory> sessions;

    @Getter
    @Builder
    @AllArgsConstructor
    public static class SessionHistory {
        private Long sessionId;
        private LocalDateTime startedAt;
        private LocalDateTime completedAt;
        private Integer totalAmount;
        private List<OrderDetail> orders;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class OrderDetail {
        private Long orderId;
        private String orderNumber;
        private Integer totalAmount;
        private List<ItemDetail> items;
        private LocalDateTime createdAt;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class ItemDetail {
        private String menuName;
        private Integer quantity;
        private Integer unitPrice;
        private Integer subtotal;
    }
}
