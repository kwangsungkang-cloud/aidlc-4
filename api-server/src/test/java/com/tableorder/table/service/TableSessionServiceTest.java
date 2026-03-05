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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class TableSessionServiceTest {

    @InjectMocks
    private TableSessionService tableSessionService;

    @Mock
    private StoreTableRepository storeTableRepository;
    @Mock
    private TableSessionRepository tableSessionRepository;
    @Mock
    private StoreService storeService;
    @Mock
    private PasswordEncoder passwordEncoder;

    private Store createStore() {
        return Store.builder()
                .id(1L)
                .storeCode("STORE001")
                .storeName("테스트 매장")
                .build();
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    @Nested
    @DisplayName("createTable - 테이블 생성")
    class CreateTable {

        @Test
        @DisplayName("테이블을 정상 생성한다")
        void 테이블_생성_성공() throws Exception {
            // given
            Long storeId = 1L;
            Store store = createStore();

            CreateTableRequest request = new CreateTableRequest();
            setField(request, "tableNumber", 5);
            setField(request, "password", "1234");

            given(storeService.getById(storeId)).willReturn(store);
            given(storeTableRepository.existsByStoreIdAndTableNumber(storeId, 5)).willReturn(false);
            given(passwordEncoder.encode("1234")).willReturn("encoded_1234");
            given(storeTableRepository.save(any(StoreTable.class))).willAnswer(invocation -> {
                StoreTable t = invocation.getArgument(0);
                Field idField = StoreTable.class.getDeclaredField("id");
                idField.setAccessible(true);
                idField.set(t, 10L);
                // createdAt 설정 (PrePersist 시뮬레이션)
                Field createdAtField = StoreTable.class.getDeclaredField("createdAt");
                createdAtField.setAccessible(true);
                createdAtField.set(t, LocalDateTime.now());
                return t;
            });

            // when
            CreateTableResponse response = tableSessionService.createTable(storeId, request);

            // then
            assertThat(response.getTableId()).isEqualTo(10L);
            assertThat(response.getStoreId()).isEqualTo(storeId);
            assertThat(response.getTableNumber()).isEqualTo(5);
        }

        @Test
        @DisplayName("이미 존재하는 테이블 번호면 예외를 던진다")
        void 중복_테이블_예외() throws Exception {
            // given
            Long storeId = 1L;
            Store store = createStore();

            CreateTableRequest request = new CreateTableRequest();
            setField(request, "tableNumber", 1);
            setField(request, "password", "1234");

            given(storeService.getById(storeId)).willReturn(store);
            given(storeTableRepository.existsByStoreIdAndTableNumber(storeId, 1)).willReturn(true);

            // when & then
            assertThatThrownBy(() -> tableSessionService.createTable(storeId, request))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.TABLE_ALREADY_EXISTS);
        }
    }

    @Nested
    @DisplayName("endSession - 세션 종료")
    class EndSession {

        @Test
        @DisplayName("활성 세션을 정상 종료한다")
        void 세션_종료_성공() {
            // given
            Long storeId = 1L;
            Long tableId = 1L;
            Store store = createStore();
            StoreTable table = StoreTable.builder()
                    .id(tableId)
                    .store(store)
                    .tableNumber(3)
                    .password("encoded")
                    .build();

            TableSession session = TableSession.builder()
                    .id(10L)
                    .storeTable(table)
                    .sessionToken("token-123")
                    .status(TableSession.Status.ACTIVE)
                    .startedAt(LocalDateTime.now().minusHours(2))
                    .expiresAt(LocalDateTime.now().plusHours(14))
                    .build();

            given(storeTableRepository.findById(tableId)).willReturn(Optional.of(table));
            given(tableSessionRepository.findByStoreTableIdAndStatus(tableId, TableSession.Status.ACTIVE))
                    .willReturn(Optional.of(session));
            given(tableSessionRepository.save(any(TableSession.class))).willAnswer(i -> i.getArgument(0));

            // when
            EndSessionResponse response = tableSessionService.endSession(tableId, storeId);

            // then
            assertThat(response.getStatus()).isEqualTo("COMPLETED");
            assertThat(response.getTableNumber()).isEqualTo(3);
            assertThat(response.getCompletedAt()).isNotNull();
        }

        @Test
        @DisplayName("다른 매장의 테이블이면 접근 거부 예외를 던진다")
        void 다른_매장_접근_거부() {
            // given
            Long storeId = 1L;
            Long tableId = 1L;
            Store otherStore = Store.builder().id(999L).storeCode("OTHER").storeName("다른 매장").build();
            StoreTable table = StoreTable.builder()
                    .id(tableId)
                    .store(otherStore)
                    .tableNumber(1)
                    .password("encoded")
                    .build();

            given(storeTableRepository.findById(tableId)).willReturn(Optional.of(table));

            // when & then
            assertThatThrownBy(() -> tableSessionService.endSession(tableId, storeId))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.ACCESS_DENIED);
        }

        @Test
        @DisplayName("활성 세션이 없으면 예외를 던진다")
        void 활성_세션_없으면_예외() {
            // given
            Long storeId = 1L;
            Long tableId = 1L;
            Store store = createStore();
            StoreTable table = StoreTable.builder()
                    .id(tableId)
                    .store(store)
                    .tableNumber(1)
                    .password("encoded")
                    .build();

            given(storeTableRepository.findById(tableId)).willReturn(Optional.of(table));
            given(tableSessionRepository.findByStoreTableIdAndStatus(tableId, TableSession.Status.ACTIVE))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> tableSessionService.endSession(tableId, storeId))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.NO_ACTIVE_SESSION);
        }
    }

    @Nested
    @DisplayName("getActiveSession - 활성 세션 조회")
    class GetActiveSession {

        @Test
        @DisplayName("활성 세션을 정상 반환한다")
        void 활성_세션_조회_성공() {
            // given
            TableSession session = TableSession.builder()
                    .id(1L)
                    .status(TableSession.Status.ACTIVE)
                    .startedAt(LocalDateTime.now())
                    .expiresAt(LocalDateTime.now().plusHours(16))
                    .build();

            given(tableSessionRepository.findById(1L)).willReturn(Optional.of(session));

            // when
            TableSession result = tableSessionService.getActiveSession(1L);

            // then
            assertThat(result.isActive()).isTrue();
        }

        @Test
        @DisplayName("종료된 세션이면 예외를 던진다")
        void 종료된_세션_예외() {
            // given
            TableSession session = TableSession.builder()
                    .id(1L)
                    .status(TableSession.Status.COMPLETED)
                    .startedAt(LocalDateTime.now().minusHours(5))
                    .expiresAt(LocalDateTime.now().plusHours(11))
                    .build();

            given(tableSessionRepository.findById(1L)).willReturn(Optional.of(session));

            // when & then
            assertThatThrownBy(() -> tableSessionService.getActiveSession(1L))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.SESSION_CLOSED);
        }

        @Test
        @DisplayName("만료된 세션이면 예외를 던진다")
        void 만료된_세션_예외() {
            // given
            TableSession session = TableSession.builder()
                    .id(1L)
                    .status(TableSession.Status.ACTIVE)
                    .startedAt(LocalDateTime.now().minusHours(20))
                    .expiresAt(LocalDateTime.now().minusHours(1))
                    .build();

            given(tableSessionRepository.findById(1L)).willReturn(Optional.of(session));

            // when & then
            assertThatThrownBy(() -> tableSessionService.getActiveSession(1L))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.SESSION_EXPIRED);
        }
    }
}
