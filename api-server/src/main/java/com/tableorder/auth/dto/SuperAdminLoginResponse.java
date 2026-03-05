package com.tableorder.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class SuperAdminLoginResponse {

    private String token;
    private Long superAdminId;
    private String username;
}
