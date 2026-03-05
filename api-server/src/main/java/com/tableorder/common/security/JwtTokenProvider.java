package com.tableorder.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

@Component
public class JwtTokenProvider {

    private final SecretKey secretKey;
    private final long expirationMs;

    public JwtTokenProvider(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-hours}") int expirationHours) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationHours * 3600L * 1000L;
    }

    public String generateTableToken(Long sessionId, Long tableId, Long storeId, Integer tableNumber) {
        Date now = new Date();
        return Jwts.builder()
                .subject("table-session")
                .claims(Map.of(
                        "sessionId", sessionId,
                        "tableId", tableId,
                        "storeId", storeId,
                        "tableNumber", tableNumber
                ))
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expirationMs))
                .signWith(secretKey)
                .compact();
    }

    public String generateAdminToken(Long adminId, Long storeId) {
        Date now = new Date();
        return Jwts.builder()
                .subject("admin")
                .claims(Map.of(
                        "adminId", adminId,
                        "storeId", storeId,
                        "role", "STORE_ADMIN"
                ))
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expirationMs))
                .signWith(secretKey)
                .compact();
    }

    public String generateSuperAdminToken(Long superAdminId) {
        Date now = new Date();
        return Jwts.builder()
                .subject("super-admin")
                .claims(Map.of(
                        "superAdminId", superAdminId,
                        "role", "SUPER_ADMIN"
                ))
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expirationMs))
                .signWith(secretKey)
                .compact();
    }

    public TokenPayload validateAndParse(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            return TokenPayload.builder()
                    .subject(claims.getSubject())
                    .sessionId(getLong(claims, "sessionId"))
                    .tableId(getLong(claims, "tableId"))
                    .storeId(getLong(claims, "storeId"))
                    .tableNumber(getInteger(claims, "tableNumber"))
                    .adminId(getLong(claims, "adminId"))
                    .superAdminId(getLong(claims, "superAdminId"))
                    .role((String) claims.get("role"))
                    .build();
        } catch (ExpiredJwtException e) {
            throw new TokenExpiredException("토큰이 만료되었습니다");
        } catch (JwtException e) {
            throw new InvalidTokenException("유효하지 않은 토큰입니다");
        }
    }

    private Long getLong(Claims claims, String key) {
        Object value = claims.get(key);
        if (value == null) return null;
        return ((Number) value).longValue();
    }

    private Integer getInteger(Claims claims, String key) {
        Object value = claims.get(key);
        if (value == null) return null;
        return ((Number) value).intValue();
    }

    public static class TokenExpiredException extends RuntimeException {
        public TokenExpiredException(String message) { super(message); }
    }

    public static class InvalidTokenException extends RuntimeException {
        public InvalidTokenException(String message) { super(message); }
    }
}
