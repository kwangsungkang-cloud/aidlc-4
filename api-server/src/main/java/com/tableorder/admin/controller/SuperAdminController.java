package com.tableorder.admin.controller;

import com.tableorder.admin.dto.*;
import com.tableorder.admin.service.AdminService;
import com.tableorder.common.dto.PageResponse;
import com.tableorder.common.security.SecurityContextUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/super-admin")
@RequiredArgsConstructor
public class SuperAdminController {

    private final AdminService adminService;

    @PostMapping("/admins")
    public ResponseEntity<Map<String, Object>> createAdmin(@Valid @RequestBody CreateAdminRequest request) {
        Long superAdminId = SecurityContextUtil.getCurrentSuperAdminId();
        return ResponseEntity.status(HttpStatus.CREATED).body(adminService.createAdmin(request, superAdminId));
    }

    @PutMapping("/admins/{adminId}")
    public ResponseEntity<Map<String, Object>> updateAdmin(
            @PathVariable Long adminId,
            @Valid @RequestBody UpdateAdminRequest request) {
        return ResponseEntity.ok(adminService.updateAdmin(adminId, request));
    }

    @DeleteMapping("/admins/{adminId}")
    public ResponseEntity<Map<String, Object>> deleteAdmin(@PathVariable Long adminId) {
        Long superAdminId = SecurityContextUtil.getCurrentSuperAdminId();
        return ResponseEntity.ok(adminService.deleteAdmin(adminId, superAdminId));
    }

    @GetMapping("/stores/{storeId}/admins")
    public ResponseEntity<Map<String, Object>> getAdminsByStore(@PathVariable Long storeId) {
        return ResponseEntity.ok(adminService.getAdminsByStore(storeId));
    }

    @GetMapping("/audit-logs")
    public ResponseEntity<PageResponse<AuditLogResponse>> getAuditLogs(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String actionType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(adminService.getAuditLogs(startDate, endDate, actionType, page, size));
    }
}
