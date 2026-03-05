package com.tableorder.table.service;

import com.tableorder.common.exception.BusinessException;
import com.tableorder.common.exception.ErrorCode;
import com.tableorder.store.entity.Store;
import com.tableorder.store.service.StoreService;
import com.tableorder.table.dto.CreateTableRequest;
import com.tableorder.table.dto.CreateTableResponse;
import com.tableorder.table.dto.EndSessionResponse;
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
class TableSessionServiceTest {

    @InjectMocks private TableSessionService tableSessionService;
    @Mock private StoreTableRepository storeTableRepository;
    @Mock private TableSessionRepository tableSessionRepository;
    @Mock private StoreService storeService;
    @Mock private PasswordEncoder passwordEncoder;

    private Store store;
    private StoreTable storeTable;

    @BeforeEach
    void setUp() {
        store = Store.builder().id(1L).storeCode("STORE001").storeName("맛있는 식당").build();
        storeTable = StoreTable.builder().id(10L).store(store).tableNumber(5).password("hashed").build();
        ReflectionTestUtils.setField(storeTable, "createdAt", LocalDateTime.now());
    }

    // ========== createTable 테스트 ==========
    @Nested
    @DisplayName("createTable - 테이블 등록")
    class CreateTableTest {

        @Test
        @DisplayName("테이블 번호 중복 → TABLE_ALREADY_EXISTS")
        void createTable_duplicate() {
            given(storeService.getById(1L)).willReturn(store);
            given(storeTableRepository.existsByStoreIdAndTableNumber(1L, 5)).willReturn(true);

            CreateTableRequest request = new CreateTableRequest();
            ReflectionTestUtils.setField(request, "tableNumber", 5);
            ReflectionTestUtils.setField(request, "password", "table1234");

            assertThatThrownBy(() -> tableSessionService.createTable(1L, request))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.TABLE_ALREADY_EXISTS);
        }

        @Test
        @DisplayName("성공 → StoreTable 생성")
        void createTable_success() {
            given(storeService.getById(1L)).willReturn(store);
            given(storeTableRepository.existsByStoreIdAndTableNumber(1L, 7)).willReturn(false);
            given(passwordEncoder.encode("table1234")).willReturn("encoded-pw");

            StoreTable savedTable = StoreTable.builder()
                    .id(20L).store(store).tableNumber(7).password("encoded-pw").build();
            ReflectionTestUtils.setField(savedTable, "createdAt", LocalDateTime.now());
            given(storeTableRepository.save(any(StoreTable.class))).willReturn(savedTable);

            CreateTableRequest request = new CreateTableRequest();
            ReflectionTestUtils.setField(request, "tableNumber", 7);
            ReflectionTestUtils.setField(request, "password", "table1234");

            CreateTableResponse response = tableSessionService.createTable(1L, request);

            assertThat(response.getTableId()).isEqualTo(20L);
            assertThat(response.getTableNumber()).isEqualTo(7);
            assertThat(response.getStoreId()).isEqualTo(1L);
        }
    }

    // ========== endSession 테스트 ==========
    @Nested
    @DisplayName("endSession - 세션 종료")
    class EndSessionTest {

        @Test
        @DisplayName("테이블 없음 → TABLE_NOT_FOUND")
        void endSession_tableNotFound() {
            given(storeTableRepository.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> tableSessionService.endSession(999L, 1L))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.TABLE_NOT_FOUND);
        }

        @Test
        @DisplayName("매장 소속 불일치 → ACCESS_DENIED")
        void endSession_accessDenied() {
            given(storeTableRepository.findById(10L)).willReturn(Optional.of(storeTable));

            assertThatThrownBy(() -> tableSessionService.endSession(10L, 999L))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.ACCESS_DENIED);
        }

        @Test
        @DisplayName("활성 세션 없음 → NO_ACTIVE_SESSION")
        void endSession_noActiveSession() {
            given(storeTableRepository.findById(10L)).willReturn(Optional.of(storeTable));
            given(tableSessionRepository.findByStoreTableIdAndStatus(10L, TableSession.Status.ACTIVE))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> tableSessionService.endSession(10L, 1L))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.NO_ACTIVE_SESSION);
        }

        @Test
        @DisplayName("성공 → 세션 COMPLETED")
        void endSession_success() {
            given(storeTableRepository.findById(10L)).willReturn(Optional.of(storeTable));

            TableSession activeSession = TableSession.builder()
                    .id(50L).storeTable(storeTable).sessionToken("uuid")
                    .status(TableSession.Status.ACTIVE)
                    .startedAt(LocalDateTime.now().minusHours(2))
                    .expiresAt(LocalDateTime.now().plusHours(14)).build();
            given(tableSessionRepository.findByStoreTableIdAndStatus(10L, TableSession.Status.ACTIVE))
                    .willReturn(Optional.of(activeSession));

            EndSessionResponse response = tableSessionService.endSession(10L, 1L);

            assertThat(response.getSessionId()).isEqualTo(50L);
            assertThat(response.getStatus()).isEqualTo("COMPLETED");
            assertThat(activeSession.getStatus()).isEqualTo(TableSession.Status.COMPLETED);
            then(tableSessionRepository).should().save(activeSession);
        }
    }

    // ========== getActiveSession 테스트 ==========
    @Nested
    @DisplayName("getActiveSession - 활성 세션 조회")
    class GetActiveSessionTest {

        @Test
        @DisplayName("세션 없음 → SESSION_CLOSED")
        void getActiveSession_notFound() {
            given(tableSessionRepository.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> tableSessionService.getActiveSession(999L))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.SESSION_CLOSED);
        }

        @Test
        @DisplayName("세션 비활성 → SESSION_CLOSED")
        void getActiveSession_notActive() {
            TableSession completedSession = TableSession.builder()
                    .id(50L).storeTable(storeTable).sessionToken("uuid")
                    .status(TableSession.Status.COMPLETED)
                    .startedAt(LocalDateTime.now().minusHours(5))
                    .expiresAt(LocalDateTime.now().plusHours(11))
                    .completedAt(LocalDateTime.now().minusHours(1)).build();
            given(tableSessionRepository.findById(50L)).willReturn(Optional.of(completedSession));

            assertThatThrownBy(() -> tableSessionService.getActiveSession(50L))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.SESSION_CLOSED);
        }

        @Test
        @DisplayName("세션 만료 → SESSION_EXPIRED")
        void getActiveSession_expired() {
            TableSession expiredSession = TableSession.builder()
                    .id(50L).storeTable(storeTable).sessionToken("uuid")
                    .status(TableSession.Status.ACTIVE)
                    .startedAt(LocalDateTime.now().minusHours(20))
                    .expiresAt(LocalDateTime.now().minusHours(4)).build();
            given(tableSessionRepository.findById(50L)).willReturn(Optional.of(expiredSession));

            assertThatThrownBy(() -> tableSessionService.getActiveSession(50L))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.SESSION_EXPIRED);
        }

        @Test
        @DisplayName("성공 → 활성 세션 반환")
        void getActiveSession_success() {
            TableSession activeSession = TableSession.builder()
                    .id(50L).storeTable(storeTable).sessionToken("uuid")
                    .status(TableSession.Status.ACTIVE)
                    .startedAt(LocalDateTime.now().minusHours(2))
                    .expiresAt(LocalDateTime.now().plusHours(14)).build();
            given(tableSessionRepository.findById(50L)).willReturn(Optional.of(activeSession));

            TableSession result = tableSessionService.getActiveSession(50L);

            assertThat(result.getId()).isEqualTo(50L);
            assertThat(result.isActive()).isTrue();
        }
    }
}
