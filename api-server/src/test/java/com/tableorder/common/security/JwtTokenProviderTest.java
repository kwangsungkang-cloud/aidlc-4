package com.tableorder.common.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;

    // 테스트용 256비트 이상 시크릿 키
    private static final String SECRET = "test-secret-key-must-be-at-least-256-bits-long-for-hmac-sha256-algorithm";

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider(SECRET, 16);
    }

    @Nested
    @DisplayName("테이블 토큰")
    class TableToken {

        @Test
        @DisplayName("테이블 토큰을 생성하고 파싱한다")
        void 토큰_생성_파싱_성공() {
            // given & when
            String token = jwtTokenProvider.generateTableToken(1L, 2L, 3L, 5);
            TokenPayload payload = jwtTokenProvider.validateAndParse(token);

            // then
            assertThat(payload.getSubject()).isEqualTo("table-session");
            assertThat(payload.getSessionId()).isEqualTo(1L);
            assertThat(payload.getTableId()).isEqualTo(2L);
            assertThat(payload.getStoreId()).isEqualTo(3L);
            assertThat(payload.getTableNumber()).isEqualTo(5);
            assertThat(payload.isTableSession()).isTrue();
            assertThat(payload.isStoreAdmin()).isFalse();
        }
    }

    @Nested
    @DisplayName("관리자 토큰")
    class AdminToken {

        @Test
        @DisplayName("관리자 토큰을 생성하고 파싱한다")
        void 관리자_토큰_생성_파싱() {
            // given & when
            String token = jwtTokenProvider.generateAdminToken(10L, 3L);
            TokenPayload payload = jwtTokenProvider.validateAndParse(token);

            // then
            assertThat(payload.getSubject()).isEqualTo("admin");
            assertThat(payload.getAdminId()).isEqualTo(10L);
            assertThat(payload.getStoreId()).isEqualTo(3L);
            assertThat(payload.isStoreAdmin()).isTrue();
            assertThat(payload.isTableSession()).isFalse();
        }
    }

    @Nested
    @DisplayName("슈퍼 관리자 토큰")
    class SuperAdminToken {

        @Test
        @DisplayName("슈퍼 관리자 토큰을 생성하고 파싱한다")
        void 슈퍼관리자_토큰_생성_파싱() {
            // given & when
            String token = jwtTokenProvider.generateSuperAdminToken(1L);
            TokenPayload payload = jwtTokenProvider.validateAndParse(token);

            // then
            assertThat(payload.getSubject()).isEqualTo("super-admin");
            assertThat(payload.getSuperAdminId()).isEqualTo(1L);
            assertThat(payload.isSuperAdmin()).isTrue();
        }
    }

    @Nested
    @DisplayName("토큰 검증 실패")
    class TokenValidationFailure {

        @Test
        @DisplayName("잘못된 토큰이면 InvalidTokenException을 던진다")
        void 잘못된_토큰_예외() {
            assertThatThrownBy(() -> jwtTokenProvider.validateAndParse("invalid-token"))
                    .isInstanceOf(JwtTokenProvider.InvalidTokenException.class);
        }

        @Test
        @DisplayName("만료된 토큰이면 TokenExpiredException을 던진다")
        void 만료된_토큰_예외() {
            // 만료 시간 0시간으로 설정한 provider
            JwtTokenProvider expiredProvider = new JwtTokenProvider(SECRET, 0);
            String token = expiredProvider.generateTableToken(1L, 1L, 1L, 1);

            assertThatThrownBy(() -> expiredProvider.validateAndParse(token))
                    .isInstanceOf(JwtTokenProvider.TokenExpiredException.class);
        }

        @Test
        @DisplayName("다른 키로 서명된 토큰이면 예외를 던진다")
        void 다른_키_서명_예외() {
            JwtTokenProvider otherProvider = new JwtTokenProvider(
                    "another-secret-key-must-be-at-least-256-bits-long-for-hmac-sha256-algorithm", 16);
            String token = otherProvider.generateTableToken(1L, 1L, 1L, 1);

            assertThatThrownBy(() -> jwtTokenProvider.validateAndParse(token))
                    .isInstanceOf(JwtTokenProvider.InvalidTokenException.class);
        }
    }
}
