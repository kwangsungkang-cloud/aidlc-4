package com.tableorder.table.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class CreateTableResponse {

    private Long tableId;
    private Long storeId;
    private Integer tableNumber;
    private LocalDateTime createdAt;
}
