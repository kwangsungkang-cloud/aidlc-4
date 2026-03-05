package com.tableorder.common.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class TokenPayloadTest {

    @Test
    @DisplayName("isTableSession - subject가 table-session → true")
    void isTableSession() {
        TokenPayload payload = TokenPayload.builder().subject("table-session").build();
        assertThat(payload.isTableSession()).isTrue();
        assertThat(payload.isStoreAdmin()).isFalse();
        assertThat(payload.isSuperAdmin()).isFalse();
    }

    @Test
    @DisplayName("isStoreAdmin - role이 STORE_ADMIN → true")
    void isStoreAdmin() {
        TokenPayload payload = TokenPayload.builder().subject("admin").role("STORE_ADMIN").build();
        assertThat(payload.isStoreAdmin()).isTrue();
        assertThat(payload.isTableSession()).isFalse();
        assertThat(payload.isSuperAdmin()).isFalse();
    }

    @Test
    @DisplayName("isSuperAdmin - role이 SUPER_ADMIN → true")
    void isSuperAdmin() {
        TokenPayload payload = TokenPayload.builder().subject("super-admin").role("SUPER_ADMIN").build();
        assertThat(payload.isSuperAdmin()).isTrue();
        assertThat(payload.isStoreAdmin()).isFalse();
        assertThat(payload.isTableSession()).isFalse();
    }
}
