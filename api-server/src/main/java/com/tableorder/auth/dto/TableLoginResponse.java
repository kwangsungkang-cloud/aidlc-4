package com.tableorder.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class TableLoginResponse {

    private String token;
    private String storeName;
    private String storeCode;
    private Integer tableNumber;
    private Long sessionId;
    private boolean isNewSession;
}
