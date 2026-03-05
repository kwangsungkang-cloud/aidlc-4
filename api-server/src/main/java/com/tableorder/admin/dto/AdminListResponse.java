package com.tableorder.admin.dto;

import com.tableorder.admin.entity.Admin;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class AdminListResponse {

    private Long adminId;
    private String username;
    private Integer loginAttempts;
    private boolean locked;
    private LocalDateTime createdAt;

    public static AdminListResponse from(Admin admin) {
        return AdminListResponse.builder()
                .adminId(admin.getId())
                .username(admin.getUsername())
                .loginAttempts(admin.getLoginAttempts())
                .locked(admin.isLocked())
                .createdAt(admin.getCreatedAt())
                .build();
    }
}
