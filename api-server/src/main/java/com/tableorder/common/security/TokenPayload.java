package com.tableorder.common.security;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class TokenPayload {

    private final String subject;
    private final Long sessionId;
    private final Long tableId;
    private final Long storeId;
    private final Integer tableNumber;
    private final Long adminId;
    private final Long superAdminId;
    private final String role;

    public boolean isTableSession() {
        return "table-session".equals(subject);
    }

    public boolean isStoreAdmin() {
        return "STORE_ADMIN".equals(role);
    }

    public boolean isSuperAdmin() {
        return "SUPER_ADMIN".equals(role);
    }
}
