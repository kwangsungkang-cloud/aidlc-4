package com.tableorder.admin.service;

import com.tableorder.admin.dto.AuditLogResponse;
import com.tableorder.admin.dto.CreateAdminRequest;
import com.tableorder.admin.dto.UpdateAdminRequest;
import com.tableorder.admin.entity.Admin;
import com.tableorder.admin.entity.AdminAuditLog;
import com.tableorder.admin.entity.SuperAdmin;
import com.tableorder.admin.repository.AdminAuditLogRepository;
import com.tableorder.admin.repository.AdminRepository;
import com.tableorder.admin.repository.SuperAdminRepository;
import com.tableorder.common.dto.PageResponse;
import com.tableorder.common.exception.BusinessException;
import com.tableorder.common.exception.ErrorCode;
import com.tableorder.store.entity.Store;
import com.tableorder.store.repository.StoreRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @InjectMocks private AdminService adminService;
    @Mock private AdminRepository adminRepository;
    @Mock private SuperAdminRepository superAdminRepository;
    @Mock private AdminAuditLogRepository auditLogRepository;
    @Mock private StoreRepository storeRepository;
    @Mock private PasswordEncoder passwordEncoder;

    private Store store;

    @BeforeEach
    void setUp() {
        store = Store.builder().id(1L).storeCode("STORE001").storeName("맛있는 식당").build();
    }

    // ========== createAdmin 테스트 ==========
    @Nested
    @DisplayName("createAdmin - 관리자 생성")
    class CreateAdminTest {

        @Test
        @DisplayName("매장 없음 → STORE_NOT_FOUND")
        void createAdmin_storeNotFound() {
            given(storeRepository.findById(999L)).willReturn(Optional.empty());

            CreateAdminRequest request = new CreateAdminRequest();
            ReflectionTestUtils.setField(request, "storeId", 999L);
            ReflectionTestUtils.setField(request, "username", "admin1");
            ReflectionTestUtils.setField(request, "password", "password123");

            assertThatThrownBy(() -> adminService.createAdmin(request, 1L))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.STORE_NOT_FOUND);
        }

        @Test
        @DisplayName("username 중복 → USERNAME_ALREADY_EXISTS")
        void createAdmin_duplicateUsername() {
            given(storeRepository.findById(1L)).willReturn(Optional.of(store));
            given(adminRepository.existsByStoreIdAndUsername(1L, "admin1")).willReturn(true);

            CreateAdminRequest request = new CreateAdminRequest();
            ReflectionTestUtils.setField(request, "storeId", 1L);
            ReflectionTestUtils.setField(request, "username", "admin1");
            ReflectionTestUtils.setField(request, "password", "password123");

            assertThatThrownBy(() -> adminService.createAdmin(request, 1L))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.USERNAME_ALREADY_EXISTS);
        }

        @Test
        @DisplayName("성공 → Admin + AuditLog 생성")
        void createAdmin_success() {
            given(storeRepository.findById(1L)).willReturn(Optional.of(store));
            given(adminRepository.existsByStoreIdAndUsername(1L, "newadmin")).willReturn(false);
            given(passwordEncoder.encode("password123")).willReturn("encoded-pw");

            given(adminRepository.save(any(Admin.class))).willAnswer(invocation -> {
                Admin a = invocation.getArgument(0);
                ReflectionTestUtils.setField(a, "id", 20L);
                ReflectionTestUtils.setField(a, "createdAt", LocalDateTime.now());
                return a;
            });

            CreateAdminRequest request = new CreateAdminRequest();
            ReflectionTestUtils.setField(request, "storeId", 1L);
            ReflectionTestUtils.setField(request, "username", "newadmin");
            ReflectionTestUtils.setField(request, "password", "password123");

            Map<String, Object> result = adminService.createAdmin(request, 1L);

            assertThat(result.get("adminId")).isEqualTo(20L);
            assertThat(result.get("username")).isEqualTo("newadmin");
            then(auditLogRepository).should().save(any(AdminAuditLog.class));
        }
    }

    // ========== updateAdmin 테스트 ==========
    @Nested
    @DisplayName("updateAdmin - 관리자 수정")
    class UpdateAdminTest {

        @Test
        @DisplayName("관리자 없음 → ADMIN_NOT_FOUND")
        void updateAdmin_notFound() {
            given(adminRepository.findById(999L)).willReturn(Optional.empty());

            UpdateAdminRequest request = new UpdateAdminRequest();
            ReflectionTestUtils.setField(request, "password", "newpass123");

            assertThatThrownBy(() -> adminService.updateAdmin(999L, request))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.ADMIN_NOT_FOUND);
        }

        @Test
        @DisplayName("성공 → 비밀번호 변경 + 잠금 초기화")
        void updateAdmin_success() {
            Admin admin = Admin.builder()
                    .id(10L).storeId(1L).username("admin1")
                    .password("old-pw").loginAttempts(3)
                    .lockedUntil(LocalDateTime.now().plusMinutes(10)).build();
            given(adminRepository.findById(10L)).willReturn(Optional.of(admin));
            given(passwordEncoder.encode("newpass123")).willReturn("new-encoded");

            UpdateAdminRequest request = new UpdateAdminRequest();
            ReflectionTestUtils.setField(request, "password", "newpass123");

            Map<String, Object> result = adminService.updateAdmin(10L, request);

            assertThat(result.get("adminId")).isEqualTo(10L);
            assertThat(admin.getLoginAttempts()).isEqualTo(0);
            assertThat(admin.getLockedUntil()).isNull();
        }
    }

    // ========== deleteAdmin 테스트 ==========
    @Nested
    @DisplayName("deleteAdmin - 관리자 삭제")
    class DeleteAdminTest {

        @Test
        @DisplayName("관리자 없음 → ADMIN_NOT_FOUND")
        void deleteAdmin_notFound() {
            given(adminRepository.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> adminService.deleteAdmin(999L, 1L))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.ADMIN_NOT_FOUND);
        }

        @Test
        @DisplayName("성공 → Admin 삭제 + AuditLog 기록")
        void deleteAdmin_success() {
            Admin admin = Admin.builder()
                    .id(10L).storeId(1L).username("admin1").password("pw").build();
            given(adminRepository.findById(10L)).willReturn(Optional.of(admin));

            Map<String, Object> result = adminService.deleteAdmin(10L, 1L);

            assertThat(result.get("success")).isEqualTo(true);
            assertThat(result.get("deletedUsername")).isEqualTo("admin1");
            then(adminRepository).should().delete(admin);
            then(auditLogRepository).should().save(argThat(log ->
                    ((AdminAuditLog) log).getActionType().equals("DELETED") &&
                    ((AdminAuditLog) log).getTargetUsername().equals("admin1")));
        }
    }

    // ========== getAdminsByStore 테스트 ==========
    @Nested
    @DisplayName("getAdminsByStore - 매장별 관리자 목록")
    class GetAdminsByStoreTest {

        @Test
        @DisplayName("매장 없음 → STORE_NOT_FOUND")
        void getAdminsByStore_storeNotFound() {
            given(storeRepository.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> adminService.getAdminsByStore(999L))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.STORE_NOT_FOUND);
        }

        @Test
        @DisplayName("성공 → 관리자 목록 반환")
        void getAdminsByStore_success() {
            given(storeRepository.findById(1L)).willReturn(Optional.of(store));

            Admin admin1 = Admin.builder().id(10L).storeId(1L).username("admin1").password("pw").loginAttempts(0).build();
            ReflectionTestUtils.setField(admin1, "createdAt", LocalDateTime.now());
            Admin admin2 = Admin.builder().id(11L).storeId(1L).username("admin2").password("pw").loginAttempts(0).build();
            ReflectionTestUtils.setField(admin2, "createdAt", LocalDateTime.now());
            given(adminRepository.findByStoreIdOrderByCreatedAtAsc(1L)).willReturn(List.of(admin1, admin2));

            Map<String, Object> result = adminService.getAdminsByStore(1L);

            assertThat(result.get("totalCount")).isEqualTo(2);
            assertThat(result.get("storeName")).isEqualTo("맛있는 식당");
        }
    }

    // ========== getAuditLogs 테스트 ==========
    @Nested
    @DisplayName("getAuditLogs - 감사 로그 조회")
    class GetAuditLogsTest {

        @Test
        @DisplayName("startDate > endDate → INVALID_DATE_RANGE")
        void getAuditLogs_invalidDateRange() {
            assertThatThrownBy(() -> adminService.getAuditLogs(
                    LocalDate.of(2026, 3, 10), LocalDate.of(2026, 3, 1), null, 0, 20))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.INVALID_DATE_RANGE);
        }

        @Test
        @DisplayName("유효하지 않은 actionType → INVALID_ACTION_TYPE")
        void getAuditLogs_invalidActionType() {
            assertThatThrownBy(() -> adminService.getAuditLogs(
                    LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 5), "INVALID", 0, 20))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.INVALID_ACTION_TYPE);
        }

        @Test
        @DisplayName("성공 → 페이징된 로그 반환 (기본값 적용)")
        void getAuditLogs_success() {
            AdminAuditLog log1 = AdminAuditLog.builder()
                    .id(1L).performedBy(1L).targetAdminId(10L)
                    .targetUsername("admin1").storeId(1L)
                    .actionType("CREATED").build();
            ReflectionTestUtils.setField(log1, "performedAt", LocalDateTime.now());

            given(auditLogRepository.findByFilters(any(), any(), isNull(), any(PageRequest.class)))
                    .willReturn(new PageImpl<>(List.of(log1), PageRequest.of(0, 20), 1));
            given(superAdminRepository.findById(1L))
                    .willReturn(Optional.of(SuperAdmin.builder().id(1L).username("superadmin").password("pw").build()));
            given(storeRepository.findById(1L)).willReturn(Optional.of(store));

            PageResponse<AuditLogResponse> result = adminService.getAuditLogs(null, null, null, 0, 20);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent().get(0).getPerformerUsername()).isEqualTo("superadmin");
        }
    }
}
