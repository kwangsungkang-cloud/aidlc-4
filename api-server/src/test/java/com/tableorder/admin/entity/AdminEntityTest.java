package com.tableorder.admin.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

class AdminEntityTest {

    @Test
    @DisplayName("isLocked - lockedUntil이 미래 → true")
    void isLocked_future_returnsTrue() {
        Admin admin = Admin.builder()
                .id(1L).storeId(1L).username("admin1").password("pw")
                .loginAttempts(5).lockedUntil(LocalDateTime.now().plusMinutes(20)).build();

        assertThat(admin.isLocked()).isTrue();
    }

    @Test
    @DisplayName("isLocked - lockedUntil이 과거 → false")
    void isLocked_past_returnsFalse() {
        Admin admin = Admin.builder()
                .id(1L).storeId(1L).username("admin1").password("pw")
                .loginAttempts(5).lockedUntil(LocalDateTime.now().minusMinutes(1)).build();

        assertThat(admin.isLocked()).isFalse();
    }

    @Test
    @DisplayName("isLocked - lockedUntil이 null → false")
    void isLocked_null_returnsFalse() {
        Admin admin = Admin.builder()
                .id(1L).storeId(1L).username("admin1").password("pw")
                .loginAttempts(0).build();

        assertThat(admin.isLocked()).isFalse();
    }

    @Test
    @DisplayName("incrementLoginAttempts - 4→5 시 잠금 설정")
    void incrementLoginAttempts_locksAt5() {
        Admin admin = Admin.builder()
                .id(1L).storeId(1L).username("admin1").password("pw")
                .loginAttempts(4).build();

        admin.incrementLoginAttempts();

        assertThat(admin.getLoginAttempts()).isEqualTo(5);
        assertThat(admin.getLockedUntil()).isNotNull();
        assertThat(admin.getLockedUntil()).isAfter(LocalDateTime.now().plusMinutes(29));
    }

    @Test
    @DisplayName("incrementLoginAttempts - 2→3 시 잠금 안됨")
    void incrementLoginAttempts_noLockBefore5() {
        Admin admin = Admin.builder()
                .id(1L).storeId(1L).username("admin1").password("pw")
                .loginAttempts(2).build();

        admin.incrementLoginAttempts();

        assertThat(admin.getLoginAttempts()).isEqualTo(3);
        assertThat(admin.getLockedUntil()).isNull();
    }

    @Test
    @DisplayName("resetLoginAttempts - 시도 횟수 + 잠금 초기화")
    void resetLoginAttempts() {
        Admin admin = Admin.builder()
                .id(1L).storeId(1L).username("admin1").password("pw")
                .loginAttempts(5).lockedUntil(LocalDateTime.now().plusMinutes(30)).build();

        admin.resetLoginAttempts();

        assertThat(admin.getLoginAttempts()).isEqualTo(0);
        assertThat(admin.getLockedUntil()).isNull();
    }

    @Test
    @DisplayName("updatePassword - 비밀번호 변경 + 잠금 초기화")
    void updatePassword() {
        Admin admin = Admin.builder()
                .id(1L).storeId(1L).username("admin1").password("old-pw")
                .loginAttempts(3).lockedUntil(LocalDateTime.now().plusMinutes(10)).build();

        admin.updatePassword("new-encoded-pw");

        assertThat(admin.getPassword()).isEqualTo("new-encoded-pw");
        assertThat(admin.getLoginAttempts()).isEqualTo(0);
        assertThat(admin.getLockedUntil()).isNull();
    }
}
