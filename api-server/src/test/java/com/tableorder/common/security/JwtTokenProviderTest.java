package com.tableorder.common.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;
    // 테스트용 256-bit 시크릿 키 (32바이트 이상)
    private static final String SECRET = "test-secret-key-for-jwt-token-generation-must-be-at-least-256-bits";

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider(SECRET, 16);
    }

    @Nested
    @DisplayName("테이블 토큰")
    class TableToken {

        @Test
        @DisplayName("테이블 토큰 생성 + 검증 성공")
        void generateAndValidateTableToken() {
            String token = jwtTokenProvider.generateTableToken(100L, 10L, 1L, 5);

            TokenPayload payload = jwtTokenProvider.validateAndParse(token);

            assertThat(payload.getSubject()).isEqualTo("table-session");
            assertThat(payload.getSessionId()).isEqualTo(100L);
            assertThat(payload.getTableId()).isEqualTo(10L);
            assertThat(payload.getStoreId()).isEqualTo(1L);
            assertThat(payload.getTableNumber()).isEqualTo(5);
            assertThat(payload.isTableSession()).isTrue();
            assertThat(payload.isStoreAdmin()).isFalse();
        }
    }

    @Nested
    @DisplayName("관리자 토큰")
    class AdminToken {

        @Test
        @DisplayName("관리자 토큰 생성 + 검증 성공")
        void generateAndValidateAdminToken() {
            String token = jwtTokenProvider.generateAdminToken(10L, 1L);

            TokenPayload payload = jwtTokenProvider.validateAndParse(token);

            assertThat(payload.getSubject()).isEqualTo("admin");
            assertThat(payload.getAdminId()).isEqualTo(10L);
            assertThat(payload.getStoreId()).isEqualTo(1L);
            assertThat(payload.getRole()).isEqualTo("STORE_ADMIN");
            assertThat(payload.isStoreAdmin()).isTrue();
            assertThat(payload.isTableSession()).isFalse();
        }
    }

    @Nested
    @DisplayName("슈퍼 관리자 토큰")
    class SuperAdminToken {

        @Test
        @DisplayName("슈퍼관리자 토큰 생성 + 검증 성공")
        void generateAndValidateSuperAdminToken() {
            String token = jwtTokenProvider.generateSuperAdminToken(1L);

            TokenPayload payload = jwtTokenProvider.validateAndParse(token);

            assertThat(payload.getSubject()).isEqualTo("super-admin");
            assertThat(payload.getSuperAdminId()).isEqualTo(1L);
            assertThat(payload.getRole()).isEqualTo("SUPER_ADMIN");
            assertThat(payload.isSuperAdmin()).isTrue();
        }
    }

    @Nested
    @DisplayName("토큰 검증 실패")
    class TokenValidationFailure {

        @Test
        @DisplayName("유효하지 않은 토큰 → InvalidTokenException")
        void validateInvalidToken() {
            assertThatThrownBy(() -> jwtTokenProvider.validateAndParse("invalid.token.here"))
                    .isInstanceOf(JwtTokenProvider.InvalidTokenException.class);
        }

        @Test
        @DisplayName("만료된 토큰 → TokenExpiredException")
        void validateExpiredToken() {
            // 만료 시간 0시간으로 설정한 provider
            JwtTokenProvider expiredProvider = new JwtTokenProvider(SECRET, 0);
            String token = expiredProvider.generateTableToken(1L, 1L, 1L, 1);

            assertThatThrownBy(() -> jwtTokenProvider.validateAndParse(token))
                    .isInstanceOf(JwtTokenProvider.TokenExpiredException.class);
        }

        @Test
        @DisplayName("다른 시크릿 키로 서명된 토큰 → InvalidTokenException")
        void validateTokenWithDifferentSecret() {
            JwtTokenProvider otherProvider = new JwtTokenProvider(
                    "another-secret-key-for-jwt-token-generation-must-be-at-least-256-bits", 16);
            String token = otherProvider.generateTableToken(1L, 1L, 1L, 1);

            assertThatThrownBy(() -> jwtTokenProvider.validateAndParse(token))
                    .isInstanceOf(JwtTokenProvider.InvalidTokenException.class);
        }
    }
}
