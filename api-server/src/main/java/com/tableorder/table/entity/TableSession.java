package com.tableorder.table.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "table_session")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class TableSession {

    public enum Status { ACTIVE, COMPLETED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "table_id", nullable = false)
    private StoreTable storeTable;

    @Column(name = "session_token", nullable = false, unique = true)
    private String sessionToken;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private Status status;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    public boolean isActive() {
        return this.status == Status.ACTIVE;
    }

    public boolean isExpired() {
        return this.expiresAt.isBefore(LocalDateTime.now());
    }

    public void complete() {
        this.status = Status.COMPLETED;
        this.completedAt = LocalDateTime.now();
    }
}
