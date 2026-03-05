package com.tableorder.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class AuditLogResponse {

    private Long logId;
    private String performerUsername;
    private Long targetAdminId;
    private String targetUsername;
    private Long storeId;
    private String storeName;
    private String actionType;
    private LocalDateTime performedAt;
}
