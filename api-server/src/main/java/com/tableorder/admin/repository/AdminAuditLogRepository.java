package com.tableorder.admin.repository;

import com.tableorder.admin.entity.AdminAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface AdminAuditLogRepository extends JpaRepository<AdminAuditLog, Long> {

    @Query("SELECT a FROM AdminAuditLog a WHERE a.performedAt >= :start AND a.performedAt < :end " +
           "AND (:actionType IS NULL OR a.actionType = :actionType) ORDER BY a.performedAt DESC")
    Page<AdminAuditLog> findByFilters(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("actionType") String actionType,
            Pageable pageable);
}
