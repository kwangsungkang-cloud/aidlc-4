package com.tableorder.auth.service;

import com.tableorder.auth.dto.TableLoginRequest;
import com.tableorder.auth.dto.TableLoginResponse;
import com.tableorder.common.exception.BusinessException;
import com.tableorder.common.exception.ErrorCode;
import com.tableorder.common.security.JwtTokenProvider;
import com.tableorder.store.entity.Store;
import com.tableorder.store.repository.StoreRepository;
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
class AuthServiceTest {

    @InjectMocks
    private AuthService authService;

    @Mock
    private StoreRepository storeRepository;
    @Mock
    private StoreTableRepository storeTableRepository;
    @Mock
    private TableSessionRepository tableSessionRepository;
    @Mock
    private JwtTokenProvider jwtTokenProvider;
    @Mock
    private PasswordEncoder passwordEncoder;

    private TableLoginRequest createLoginRequest() throws Exception {
        TableLoginRequest request = new TableLoginRequest();
        setField(request, "storeCode", "STORE001");
        setField(request, "tableNumber", 1);
        setField(request, "password", "1234");
        return request;
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private Store createStore() {
        return Store.builder()
                .id(1L)
                .storeCode("STORE001")
                .storeName("테스트 매장")
                .build();
    }

    private StoreTable createTable(Store store) {
        return StoreTable.builder()
                .id(1L)
                .store(store)
                .tableNumber(1)
                .password("encoded_password")
                .build();
    }

    @Nested
    @DisplayName("loginTable - 테이블 로그인")
    class LoginTable {

        @Test
        @DisplayName("신규 세션으로 정상 로그인한다")
        void 신규_세션_로그인_성공() throws Exception {
            // given
            TableLoginRequest request = createLoginRequest();
            Store store = createStore();
            StoreTable table = createTable(store);

            given(storeRepository.findByStoreCode("STORE001")).willReturn(Optional.of(store));
            given(storeTableRepository.findByStoreIdAndTableNumber(1L, 1)).willReturn(Optional.of(table));
            given(passwordEncoder.matches("1234", "encoded_password")).willReturn(true);
            given(tableSessionRepository.findByStoreTableIdAndStatus(1L, TableSession.Status.ACTIVE))
                    .willReturn(Optional.empty());
            given(tableSessionRepository.save(any(TableSession.class))).willAnswer(invocation -> {
                TableSession s = invocation.getArgument(0);
                // 리플렉션으로 id 설정
                Field idField = TableSession.class.getDeclaredField("id");
                idField.setAccessible(true);
                idField.set(s, 100L);
                return s;
            });
            given(jwtTokenProvider.generateTableToken(100L, 1L, 1L, 1)).willReturn("jwt-token");

            // when
            TableLoginResponse response = authService.loginTable(request);

            // then
            assertThat(response.getToken()).isEqualTo("jwt-token");
            assertThat(response.getStoreName()).isEqualTo("테스트 매장");
            assertThat(response.getTableNumber()).isEqualTo(1);
            assertThat(response.isNewSession()).isTrue();
        }

        @Test
        @DisplayName("기존 활성 세션이 있으면 재사용한다")
        void 기존_세션_재사용() throws Exception {
            // given
            TableLoginRequest request = createLoginRequest();
            Store store = createStore();
            StoreTable table = createTable(store);

            TableSession existingSession = TableSession.builder()
                    .id(50L)
                    .storeTable(table)
                    .sessionToken("existing-token")
                    .status(TableSession.Status.ACTIVE)
                    .startedAt(LocalDateTime.now().minusHours(1))
                    .expiresAt(LocalDateTime.now().plusHours(15))
                    .build();
            // 리플렉션으로 id 설정
            Field idField = TableSession.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(existingSession, 50L);

            given(storeRepository.findByStoreCode("STORE001")).willReturn(Optional.of(store));
            given(storeTableRepository.findByStoreIdAndTableNumber(1L, 1)).willReturn(Optional.of(table));
            given(passwordEncoder.matches("1234", "encoded_password")).willReturn(true);
            given(tableSessionRepository.findByStoreTableIdAndStatus(1L, TableSession.Status.ACTIVE))
                    .willReturn(Optional.of(existingSession));
            given(jwtTokenProvider.generateTableToken(50L, 1L, 1L, 1)).willReturn("jwt-token");

            // when
            TableLoginResponse response = authService.loginTable(request);

            // then
            assertThat(response.isNewSession()).isFalse();
            assertThat(response.getSessionId()).isEqualTo(50L);
        }

        @Test
        @DisplayName("매장 코드가 잘못되면 예외를 던진다")
        void 매장_없으면_예외() throws Exception {
            // given
            TableLoginRequest request = createLoginRequest();
            given(storeRepository.findByStoreCode("STORE001")).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> authService.loginTable(request))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.STORE_NOT_FOUND);
        }

        @Test
        @DisplayName("비밀번호가 틀리면 예외를 던진다")
        void 비밀번호_불일치_예외() throws Exception {
            // given
            TableLoginRequest request = createLoginRequest();
            Store store = createStore();
            StoreTable table = createTable(store);

            given(storeRepository.findByStoreCode("STORE001")).willReturn(Optional.of(store));
            given(storeTableRepository.findByStoreIdAndTableNumber(1L, 1)).willReturn(Optional.of(table));
            given(passwordEncoder.matches("1234", "encoded_password")).willReturn(false);

            // when & then
            assertThatThrownBy(() -> authService.loginTable(request))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.INVALID_CREDENTIALS);
        }
    }
}
