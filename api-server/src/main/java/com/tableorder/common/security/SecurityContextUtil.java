package com.tableorder.common.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityContextUtil {

    private SecurityContextUtil() {}

    public static TokenPayload getCurrentPayload() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof TokenPayload)) {
            throw new IllegalStateException("인증 정보가 없습니다");
        }
        return (TokenPayload) auth.getPrincipal();
    }

    public static Long getCurrentStoreId() {
        return getCurrentPayload().getStoreId();
    }

    public static Long getCurrentSessionId() {
        return getCurrentPayload().getSessionId();
    }

    public static Long getCurrentAdminId() {
        return getCurrentPayload().getAdminId();
    }

    public static Long getCurrentSuperAdminId() {
        return getCurrentPayload().getSuperAdminId();
    }
}
