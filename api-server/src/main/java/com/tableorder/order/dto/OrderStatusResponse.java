package com.tableorder.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class OrderStatusResponse {

    private Long orderId;
    private String orderNumber;
    private String previousStatus;
    private String status;
    private LocalDateTime updatedAt;
}
