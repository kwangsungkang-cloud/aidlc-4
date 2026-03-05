package com.tableorder.table.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class EndSessionResponse {

    private Long sessionId;
    private Integer tableNumber;
    private String status;
    private LocalDateTime completedAt;
    private Integer totalOrderAmount;
    private Integer orderCount;
}
