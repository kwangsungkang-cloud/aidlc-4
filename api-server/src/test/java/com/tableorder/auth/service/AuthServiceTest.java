package com.tableorder.auth.service;

import com.tableorder.admin.entity.Admin;
import com.tableorder.admin.entity.SuperAdmin;
import com.tableorder.admin.repository.AdminRepository;
import com.tableorder.admin.repository.SuperAdminRepository;
import com.tableorder.auth.dto.*;
import com.tableorder.common.exception.BusinessException;
import com.tableorder.common.exception.ErrorCode;
import com.tableorder.common.security.JwtTokenProvider;
import com.tableorder.store.entity.Store;
import com.tableorder.store.repository.StoreRepository;
import com.tableorder.table.entity.StoreTable;
import com.tableorder.table.entity.TableSession;
import com.tableorder.table.repository.StoreTableRepository;
import com.tableorder.table.repository.TableSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @InjectMocks private AuthService authService;
    @Mock private StoreRepository storeRepository;
    @Mock private StoreTableRepository storeTableRepository;
    @Mock private TableSessionRepository tableSessionRepository;
    @Mock private AdminRepository adminRepository;
    @Mock private SuperAdminRepository superAdminRepository;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private PasswordEncoder passwordEncoder;

    private Store store;
    private StoreTable storeTable;

    @BeforeEach
    void setUp() {
        store = Store.builder().id(1L).storeCode("STORE001").storeName("맛있는 식당").build();
        storeTable = StoreTable.builder().id(10L).store(store).tableNumber(5).password("hashed").build();
    }

    // ========== 테이블 로그인 테스트 ==========
    @Nested
    @DisplayName("loginTable - 테이블 태블릿 로그인")
    class LoginTableTest {

        @Test
        @DisplayName("매장 없음 → STORE_NOT_FOUND")
        void loginTable_storeNotFound() {
            given(storeRepository.findByStoreCode("INVALID")).willReturn(Optional.empty());

            TableLoginRequest request = new TableLoginRequest();
            ReflectionTestUtils.setField(request, "storeCode", "INVALID");
            ReflectionTestUtils.setField(request, "tableNumber", 1);
            ReflectionTestUtils.setField(request, "password", "pass");

            assertThatThrownBy(() -> authService.loginTable(request))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.STORE_NOT_FOUND);
        }

        @Test
        @DisplayName("테이블 없음 → TABLE_NOT_FOUND")
        void loginTable_tableNotFound() {
            given(storeRepository.findByStoreCode("STORE001")).willReturn(Optional.of(store));
            given(storeTableRepository.findByStoreIdAndTableNumber(1L, 99)).willReturn(Optional.empty());

            TableLoginRequest request = new TableLoginRequest();
            ReflectionTestUtils.setField(request, "storeCode", "STORE001");
            ReflectionTestUtils.setField(request, "tableNumber", 99);
            ReflectionTestUtils.setField(request, "password", "pass");

            assertThatThrownBy(() -> authService.loginTable(request))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.TABLE_NOT_FOUND);
        }

        @Test
        @DisplayName("비밀번호 불일치 → INVALID_CREDENTIALS")
        void loginTable_wrongPassword() {
            given(storeRepository.findByStoreCode("STORE001")).willReturn(Optional.of(store));
            given(storeTableRepository.findByStoreIdAndTableNumber(1L, 5)).willReturn(Optional.of(storeTable));
            given(passwordEncoder.matches("wrong", "hashed")).willReturn(false);

            TableLoginRequest request = new TableLoginRequest();
            ReflectionTestUtils.setField(request, "storeCode", "STORE001");
            ReflectionTestUtils.setField(request, "tableNumber", 5);
            ReflectionTestUtils.setField(request, "password", "wrong");

            assertThatThrownBy(() -> authService.loginTable(request))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.INVALID_CREDENTIALS);
        }

        @Test
        @DisplayName("활성 세션 없음 → 새 세션 생성 (isNewSession=true)")
        void loginTable_noActiveSession_createsNew() {
            given(storeRepository.findByStoreCode("STORE001")).willReturn(Optional.of(store));
            given(storeTableRepository.findByStoreIdAndTableNumber(1L, 5)).willReturn(Optional.of(storeTable));
            given(passwordEncoder.matches("correct", "hashed")).willReturn(true);
            given(tableSessionRepository.findByStoreTableIdAndStatus(10L, TableSession.Status.ACTIVE))
                    .willReturn(Optional.empty());

            TableSession newSession = TableSession.builder()
                    .id(100L).storeTable(storeTable).sessionToken("uuid")
                    .status(TableSession.Status.ACTIVE)
                    .startedAt(LocalDateTime.now()).expiresAt(LocalDateTime.now().plusHours(16)).build();
            given(tableSessionRepository.save(any(TableSession.class))).willReturn(newSession);
            given(jwtTokenProvider.generateTableToken(100L, 10L, 1L, 5)).willReturn("jwt-token");

            TableLoginRequest request = new TableLoginRequest();
            ReflectionTestUtils.setField(request, "storeCode", "STORE001");
            ReflectionTestUtils.setField(request, "tableNumber", 5);
            ReflectionTestUtils.setField(request, "password", "correct");

            TableLoginResponse response = authService.loginTable(request);

            assertThat(response.getToken()).isEqualTo("jwt-token");
            assertThat(response.isNewSession()).isTrue();
            assertThat(response.getSessionId()).isEqualTo(100L);
        }

        @Test
        @DisplayName("활성 세션 존재 + 유효 → 기존 세션 재사용 (isNewSession=false)")
        void loginTable_activeSessionValid_reuses() {
            given(storeRepository.findByStoreCode("STORE001")).willReturn(Optional.of(store));
            given(storeTableRepository.findByStoreIdAndTableNumber(1L, 5)).willReturn(Optional.of(storeTable));
            given(passwordEncoder.matches("correct", "hashed")).willReturn(true);

            TableSession activeSession = TableSession.builder()
                    .id(50L).storeTable(storeTable).sessionToken("existing-uuid")
                    .status(TableSession.Status.ACTIVE)
                    .startedAt(LocalDateTime.now().minusHours(2))
                    .expiresAt(LocalDateTime.now().plusHours(14)).build();
            given(tableSessionRepository.findByStoreTableIdAndStatus(10L, TableSession.Status.ACTIVE))
                    .willReturn(Optional.of(activeSession));
            given(jwtTokenProvider.generateTableToken(50L, 10L, 1L, 5)).willReturn("jwt-reuse");

            TableLoginRequest request = new TableLoginRequest();
            ReflectionTestUtils.setField(request, "storeCode", "STORE001");
            ReflectionTestUtils.setField(request, "tableNumber", 5);
            ReflectionTestUtils.setField(request, "password", "correct");

            TableLoginResponse response = authService.loginTable(request);

            assertThat(response.getToken()).isEqualTo("jwt-reuse");
            assertThat(response.isNewSession()).isFalse();
            assertThat(response.getSessionId()).isEqualTo(50L);
        }

        @Test
        @DisplayName("활성 세션 존재 + 만료됨 → 기존 세션 COMPLETED + 새 세션 생성")
        void loginTable_activeSessionExpired_createsNew() {
            given(storeRepository.findByStoreCode("STORE001")).willReturn(Optional.of(store));
            given(storeTableRepository.findByStoreIdAndTableNumber(1L, 5)).willReturn(Optional.of(storeTable));
            given(passwordEncoder.matches("correct", "hashed")).willReturn(true);

            TableSession expiredSession = TableSession.builder()
                    .id(30L).storeTable(storeTable).sessionToken("old-uuid")
                    .status(TableSession.Status.ACTIVE)
                    .startedAt(LocalDateTime.now().minusHours(20))
                    .expiresAt(LocalDateTime.now().minusHours(4)).build();
            given(tableSessionRepository.findByStoreTableIdAndStatus(10L, TableSession.Status.ACTIVE))
                    .willReturn(Optional.of(expiredSession));
            given(tableSessionRepository.save(any(TableSession.class))).willAnswer(invocation -> {
                TableSession s = invocation.getArgument(0);
                if (s.getId() == null || s.getId() != 30L) {
                    ReflectionTestUtils.setField(s, "id", 101L);
                }
                return s;
            });
            given(jwtTokenProvider.generateTableToken(eq(101L), eq(10L), eq(1L), eq(5))).willReturn("jwt-new");

            TableLoginRequest request = new TableLoginRequest();
            ReflectionTestUtils.setField(request, "storeCode", "STORE001");
            ReflectionTestUtils.setField(request, "tableNumber", 5);
            ReflectionTestUtils.setField(request, "password", "correct");

            TableLoginResponse response = authService.loginTable(request);

            assertThat(response.getToken()).isEqualTo("jwt-new");
            assertThat(response.isNewSession()).isTrue();
            then(tableSessionRepository).should(times(2)).save(any(TableSession.class));
        }
    }

    // ========== 관리자 로그인 테스트 ==========
    @Nested
    @DisplayName("loginAdmin - 매장 관리자 로그인")
    class LoginAdminTest {

        private Admin admin;

        @BeforeEach
        void setUp() {
            admin = Admin.builder()
                    .id(10L).storeId(1L).username("admin1")
                    .password("hashed-pw").loginAttempts(0).build();
        }

        @Test
        @DisplayName("매장 없음 → INVALID_CREDENTIALS")
        void loginAdmin_storeNotFound() {
            given(storeRepository.findByStoreCode("INVALID")).willReturn(Optional.empty());

            AdminLoginRequest request = new AdminLoginRequest();
            ReflectionTestUtils.setField(request, "storeCode", "INVALID");
            ReflectionTestUtils.setField(request, "username", "admin1");
            ReflectionTestUtils.setField(request, "password", "pass");

            assertThatThrownBy(() -> authService.loginAdmin(request))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.INVALID_CREDENTIALS);
        }

        @Test
        @DisplayName("관리자 없음 → INVALID_CREDENTIALS")
        void loginAdmin_adminNotFound() {
            given(storeRepository.findByStoreCode("STORE001")).willReturn(Optional.of(store));
            given(adminRepository.findByStoreIdAndUsername(1L, "unknown")).willReturn(Optional.empty());

            AdminLoginRequest request = new AdminLoginRequest();
            ReflectionTestUtils.setField(request, "storeCode", "STORE001");
            ReflectionTestUtils.setField(request, "username", "unknown");
            ReflectionTestUtils.setField(request, "password", "pass");

            assertThatThrownBy(() -> authService.loginAdmin(request))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.INVALID_CREDENTIALS);
        }

        @Test
        @DisplayName("계정 잠금 상태 → ACCOUNT_LOCKED")
        void loginAdmin_accountLocked() {
            Admin lockedAdmin = Admin.builder()
                    .id(10L).storeId(1L).username("admin1")
                    .password("hashed-pw").loginAttempts(5)
                    .lockedUntil(LocalDateTime.now().plusMinutes(20)).build();

            given(storeRepository.findByStoreCode("STORE001")).willReturn(Optional.of(store));
            given(adminRepository.findByStoreIdAndUsername(1L, "admin1")).willReturn(Optional.of(lockedAdmin));

            AdminLoginRequest request = new AdminLoginRequest();
            ReflectionTestUtils.setField(request, "storeCode", "STORE001");
            ReflectionTestUtils.setField(request, "username", "admin1");
            ReflectionTestUtils.setField(request, "password", "pass");

            assertThatThrownBy(() -> authService.loginAdmin(request))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.ACCOUNT_LOCKED);
        }

        @Test
        @DisplayName("비밀번호 불일치 → 시도 횟수 증가 + INVALID_CREDENTIALS")
        void loginAdmin_wrongPassword_incrementAttempts() {
            given(storeRepository.findByStoreCode("STORE001")).willReturn(Optional.of(store));
            given(adminRepository.findByStoreIdAndUsername(1L, "admin1")).willReturn(Optional.of(admin));
            given(passwordEncoder.matches("wrong", "hashed-pw")).willReturn(false);

            AdminLoginRequest request = new AdminLoginRequest();
            ReflectionTestUtils.setField(request, "storeCode", "STORE001");
            ReflectionTestUtils.setField(request, "username", "admin1");
            ReflectionTestUtils.setField(request, "password", "wrong");

            assertThatThrownBy(() -> authService.loginAdmin(request))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.INVALID_CREDENTIALS);

            assertThat(admin.getLoginAttempts()).isEqualTo(1);
            then(adminRepository).should().save(admin);
        }

        @Test
        @DisplayName("5회 실패 → 계정 잠금 (lockedUntil 설정)")
        void loginAdmin_fifthFailure_locksAccount() {
            Admin almostLockedAdmin = Admin.builder()
                    .id(10L).storeId(1L).username("admin1")
                    .password("hashed-pw").loginAttempts(4).build();

            given(storeRepository.findByStoreCode("STORE001")).willReturn(Optional.of(store));
            given(adminRepository.findByStoreIdAndUsername(1L, "admin1")).willReturn(Optional.of(almostLockedAdmin));
            given(passwordEncoder.matches("wrong", "hashed-pw")).willReturn(false);

            AdminLoginRequest request = new AdminLoginRequest();
            ReflectionTestUtils.setField(request, "storeCode", "STORE001");
            ReflectionTestUtils.setField(request, "username", "admin1");
            ReflectionTestUtils.setField(request, "password", "wrong");

            assertThatThrownBy(() -> authService.loginAdmin(request))
                    .isInstanceOf(BusinessException.class);

            assertThat(almostLockedAdmin.getLoginAttempts()).isEqualTo(5);
            assertThat(almostLockedAdmin.getLockedUntil()).isNotNull();
            assertThat(almostLockedAdmin.isLocked()).isTrue();
        }

        @Test
        @DisplayName("로그인 성공 → 시도 횟수 초기화 + JWT 발급")
        void loginAdmin_success() {
            Admin adminWith2Attempts = Admin.builder()
                    .id(10L).storeId(1L).username("admin1")
                    .password("hashed-pw").loginAttempts(2).build();

            given(storeRepository.findByStoreCode("STORE001")).willReturn(Optional.of(store));
            given(adminRepository.findByStoreIdAndUsername(1L, "admin1")).willReturn(Optional.of(adminWith2Attempts));
            given(passwordEncoder.matches("correct", "hashed-pw")).willReturn(true);
            given(jwtTokenProvider.generateAdminToken(10L, 1L)).willReturn("admin-jwt");

            AdminLoginRequest request = new AdminLoginRequest();
            ReflectionTestUtils.setField(request, "storeCode", "STORE001");
            ReflectionTestUtils.setField(request, "username", "admin1");
            ReflectionTestUtils.setField(request, "password", "correct");

            AdminLoginResponse response = authService.loginAdmin(request);

            assertThat(response.getToken()).isEqualTo("admin-jwt");
            assertThat(response.getAdminId()).isEqualTo(10L);
            assertThat(adminWith2Attempts.getLoginAttempts()).isEqualTo(0);
        }
    }

    // ========== 슈퍼 관리자 로그인 테스트 ==========
    @Nested
    @DisplayName("loginSuperAdmin - 슈퍼 관리자 로그인")
    class LoginSuperAdminTest {

        @Test
        @DisplayName("슈퍼관리자 없음 → INVALID_CREDENTIALS")
        void loginSuperAdmin_notFound() {
            given(superAdminRepository.findByUsername("unknown")).willReturn(Optional.empty());

            SuperAdminLoginRequest request = new SuperAdminLoginRequest();
            ReflectionTestUtils.setField(request, "username", "unknown");
            ReflectionTestUtils.setField(request, "password", "pass");

            assertThatThrownBy(() -> authService.loginSuperAdmin(request))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.INVALID_CREDENTIALS);
        }

        @Test
        @DisplayName("비밀번호 불일치 → INVALID_CREDENTIALS")
        void loginSuperAdmin_wrongPassword() {
            SuperAdmin superAdmin = SuperAdmin.builder()
                    .id(1L).username("superadmin").password("hashed").build();
            given(superAdminRepository.findByUsername("superadmin")).willReturn(Optional.of(superAdmin));
            given(passwordEncoder.matches("wrong", "hashed")).willReturn(false);

            SuperAdminLoginRequest request = new SuperAdminLoginRequest();
            ReflectionTestUtils.setField(request, "username", "superadmin");
            ReflectionTestUtils.setField(request, "password", "wrong");

            assertThatThrownBy(() -> authService.loginSuperAdmin(request))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.INVALID_CREDENTIALS);
        }

        @Test
        @DisplayName("로그인 성공 → JWT 발급")
        void loginSuperAdmin_success() {
            SuperAdmin superAdmin = SuperAdmin.builder()
                    .id(1L).username("superadmin").password("hashed").build();
            given(superAdminRepository.findByUsername("superadmin")).willReturn(Optional.of(superAdmin));
            given(passwordEncoder.matches("correct", "hashed")).willReturn(true);
            given(jwtTokenProvider.generateSuperAdminToken(1L)).willReturn("super-jwt");

            SuperAdminLoginRequest request = new SuperAdminLoginRequest();
            ReflectionTestUtils.setField(request, "username", "superadmin");
            ReflectionTestUtils.setField(request, "password", "correct");

            SuperAdminLoginResponse response = authService.loginSuperAdmin(request);

            assertThat(response.getToken()).isEqualTo("super-jwt");
            assertThat(response.getSuperAdminId()).isEqualTo(1L);
            assertThat(response.getUsername()).isEqualTo("superadmin");
        }
    }
}
