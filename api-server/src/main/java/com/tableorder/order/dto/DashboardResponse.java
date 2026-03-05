package com.tableorder.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class DashboardResponse {

    private Long storeId;
    private List<TableSummary> tables;

    @Getter
    @Builder
    @AllArgsConstructor
    public static class TableSummary {
        private Long tableId;
        private Integer tableNumber;
        private Long sessionId;
        private String sessionStatus;
        private Integer totalOrderAmount;
        private Integer orderCount;
        private List<RecentOrder> recentOrders;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class RecentOrder {
        private Long orderId;
        private String orderNumber;
        private String status;
        private Integer totalAmount;
        private String itemSummary;
        private LocalDateTime createdAt;
    }
}
