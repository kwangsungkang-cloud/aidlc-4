package com.tableorder.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class AdminLoginResponse {

    private String token;
    private String storeName;
    private String storeCode;
    private Long adminId;
    private String username;
}
