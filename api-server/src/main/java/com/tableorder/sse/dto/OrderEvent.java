package com.tableorder.sse.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrderEvent {

    private String eventType;

    // 공통
    private Long orderId;
    private String orderNumber;
    private Integer tableNumber;

    // NEW_ORDER
    private Integer totalAmount;
    private Integer itemCount;
    private String itemSummary;
    private LocalDateTime createdAt;

    // ORDER_STATUS_CHANGED
    private String previousStatus;
    private String newStatus;
    private LocalDateTime updatedAt;

    // ORDER_DELETED
    private Integer deletedAmount;
    private LocalDateTime deletedAt;

    // SESSION_COMPLETED
    private Long tableId;
    private Long sessionId;
    private LocalDateTime completedAt;
    private Integer totalOrderAmount;
    private Integer orderCount;

    // CONNECTED
    private Long storeId;
    private LocalDateTime connectedAt;
}
