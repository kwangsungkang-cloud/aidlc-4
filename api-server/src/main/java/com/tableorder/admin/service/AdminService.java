package com.tableorder.admin.service;

import com.tableorder.admin.dto.*;
import com.tableorder.admin.entity.Admin;
import com.tableorder.admin.entity.AdminAuditLog;
import com.tableorder.admin.repository.AdminAuditLogRepository;
import com.tableorder.admin.repository.AdminRepository;
import com.tableorder.admin.repository.SuperAdminRepository;
import com.tableorder.common.dto.PageResponse;
import com.tableorder.common.exception.BusinessException;
import com.tableorder.common.exception.ErrorCode;
import com.tableorder.store.entity.Store;
import com.tableorder.store.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {

    private final AdminRepository adminRepository;
    private final SuperAdminRepository superAdminRepository;
    private final AdminAuditLogRepository auditLogRepository;
    private final StoreRepository storeRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public Map<String, Object> createAdmin(CreateAdminRequest request, Long superAdminId) {
        Store store = storeRepository.findById(request.getStoreId())
                .orElseThrow(() -> new BusinessException(ErrorCode.STORE_NOT_FOUND));

        if (adminRepository.existsByStoreIdAndUsername(request.getStoreId(), request.getUsername())) {
            throw new BusinessException(ErrorCode.USERNAME_ALREADY_EXISTS);
        }

        Admin admin = Admin.builder()
                .storeId(store.getId())
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .build();
        adminRepository.save(admin);

        AdminAuditLog auditLog = AdminAuditLog.builder()
                .performedBy(superAdminId)
                .targetAdminId(admin.getId())
                .targetUsername(admin.getUsername())
                .storeId(store.getId())
                .actionType("CREATED")
                .build();
        auditLogRepository.save(auditLog);

        log.info("Admin created: adminId={}, storeId={}, username={}", admin.getId(), store.getId(), admin.getUsername());

        return Map.of(
                "adminId", admin.getId(),
                "storeId", store.getId(),
                "username", admin.getUsername(),
                "createdAt", admin.getCreatedAt()
        );
    }

    @Transactional
    public Map<String, Object> updateAdmin(Long adminId, UpdateAdminRequest request) {
        Admin admin = adminRepository.findById(adminId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ADMIN_NOT_FOUND));

        admin.updatePassword(passwordEncoder.encode(request.getPassword()));
        adminRepository.save(admin);

        log.info("Admin password updated: adminId={}", adminId);

        return Map.of(
                "adminId", admin.getId(),
                "storeId", admin.getStoreId(),
                "username", admin.getUsername(),
                "message", "비밀번호가 변경되었습니다"
        );
    }

    @Transactional
    public Map<String, Object> deleteAdmin(Long adminId, Long superAdminId) {
        Admin admin = adminRepository.findById(adminId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ADMIN_NOT_FOUND));

        String targetUsername = admin.getUsername();
        Long targetStoreId = admin.getStoreId();

        adminRepository.delete(admin);

        AdminAuditLog auditLog = AdminAuditLog.builder()
                .performedBy(superAdminId)
                .targetAdminId(adminId)
                .targetUsername(targetUsername)
                .storeId(targetStoreId)
                .actionType("DELETED")
                .build();
        auditLogRepository.save(auditLog);

        log.info("Admin deleted: adminId={}, username={}", adminId, targetUsername);

        return Map.of(
                "success", true,
                "deletedAdminId", adminId,
                "deletedUsername", targetUsername
        );
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getAdminsByStore(Long storeId) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.STORE_NOT_FOUND));

        List<AdminListResponse> admins = adminRepository.findByStoreIdOrderByCreatedAtAsc(storeId)
                .stream()
                .map(AdminListResponse::from)
                .toList();

        return Map.of(
                "storeId", store.getId(),
                "storeName", store.getStoreName(),
                "admins", admins,
                "totalCount", admins.size()
        );
    }

    @Transactional(readOnly = true)
    public PageResponse<AuditLogResponse> getAuditLogs(
            LocalDate startDate, LocalDate endDate, String actionType, int page, int size) {

        if (startDate == null) startDate = LocalDate.now().minusDays(30);
        if (endDate == null) endDate = LocalDate.now();

        if (startDate.isAfter(endDate)) {
            throw new BusinessException(ErrorCode.INVALID_DATE_RANGE);
        }

        if (actionType != null && !actionType.equals("CREATED") && !actionType.equals("DELETED")) {
            throw new BusinessException(ErrorCode.INVALID_ACTION_TYPE);
        }

        size = Math.min(Math.max(size, 1), 100);
        page = Math.max(page, 0);

        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.plusDays(1).atStartOfDay();

        Page<AdminAuditLog> logPage = auditLogRepository.findByFilters(start, end, actionType, PageRequest.of(page, size));

        List<AuditLogResponse> logs = logPage.getContent().stream()
                .map(this::toAuditLogResponse)
                .toList();

        return PageResponse.of(logs, page, size, logPage.getTotalElements());
    }

    private AuditLogResponse toAuditLogResponse(AdminAuditLog log) {
        String performerUsername = superAdminRepository.findById(log.getPerformedBy())
                .map(sa -> sa.getUsername())
                .orElse("unknown");

        String storeName = storeRepository.findById(log.getStoreId())
                .map(s -> s.getStoreName())
                .orElse("unknown");

        return AuditLogResponse.builder()
                .logId(log.getId())
                .performerUsername(performerUsername)
                .targetAdminId(log.getTargetAdminId())
                .targetUsername(log.getTargetUsername())
                .storeId(log.getStoreId())
                .storeName(storeName)
                .actionType(log.getActionType())
                .performedAt(log.getPerformedAt())
                .build();
    }
}
