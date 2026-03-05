package com.tableorder.admin.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "admin_audit_log")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class AdminAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "performed_by", nullable = false)
    private Long performedBy;

    @Column(name = "target_admin_id")
    private Long targetAdminId;

    @Column(name = "target_username", nullable = false, length = 50)
    private String targetUsername;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @Column(name = "action_type", nullable = false, length = 20)
    private String actionType;

    @Column(name = "performed_at", nullable = false, updatable = false)
    private LocalDateTime performedAt;

    @PrePersist
    protected void onCreate() {
        this.performedAt = LocalDateTime.now();
    }
}
