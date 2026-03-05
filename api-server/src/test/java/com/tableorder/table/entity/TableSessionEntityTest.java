package com.tableorder.table.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

class TableSessionEntityTest {

    @Test
    @DisplayName("isActive - ACTIVE 상태 → true")
    void isActive_active_returnsTrue() {
        TableSession session = TableSession.builder()
                .status(TableSession.Status.ACTIVE)
                .startedAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusHours(16))
                .sessionToken("uuid").build();

        assertThat(session.isActive()).isTrue();
    }

    @Test
    @DisplayName("isActive - COMPLETED 상태 → false")
    void isActive_completed_returnsFalse() {
        TableSession session = TableSession.builder()
                .status(TableSession.Status.COMPLETED)
                .startedAt(LocalDateTime.now().minusHours(5))
                .expiresAt(LocalDateTime.now().plusHours(11))
                .sessionToken("uuid").build();

        assertThat(session.isActive()).isFalse();
    }

    @Test
    @DisplayName("isExpired - 만료 시간 지남 → true")
    void isExpired_past_returnsTrue() {
        TableSession session = TableSession.builder()
                .status(TableSession.Status.ACTIVE)
                .startedAt(LocalDateTime.now().minusHours(20))
                .expiresAt(LocalDateTime.now().minusHours(4))
                .sessionToken("uuid").build();

        assertThat(session.isExpired()).isTrue();
    }

    @Test
    @DisplayName("isExpired - 만료 시간 안 지남 → false")
    void isExpired_future_returnsFalse() {
        TableSession session = TableSession.builder()
                .status(TableSession.Status.ACTIVE)
                .startedAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusHours(16))
                .sessionToken("uuid").build();

        assertThat(session.isExpired()).isFalse();
    }

    @Test
    @DisplayName("complete - 상태 COMPLETED + completedAt 설정")
    void complete_setsStatusAndTime() {
        TableSession session = TableSession.builder()
                .status(TableSession.Status.ACTIVE)
                .startedAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusHours(16))
                .sessionToken("uuid").build();

        session.complete();

        assertThat(session.getStatus()).isEqualTo(TableSession.Status.COMPLETED);
        assertThat(session.getCompletedAt()).isNotNull();
        assertThat(session.isActive()).isFalse();
    }
}
