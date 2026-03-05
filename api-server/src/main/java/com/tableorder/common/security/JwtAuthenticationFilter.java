package com.tableorder.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = extractToken(request);

        if (token != null) {
            try {
                TokenPayload payload = jwtTokenProvider.validateAndParse(token);
                List<SimpleGrantedAuthority> authorities = resolveAuthorities(payload);

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(payload, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (JwtTokenProvider.TokenExpiredException e) {
                log.debug("Token expired: {}", e.getMessage());
            } catch (JwtTokenProvider.InvalidTokenException e) {
                log.debug("Invalid token: {}", e.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    private List<SimpleGrantedAuthority> resolveAuthorities(TokenPayload payload) {
        if (payload.isSuperAdmin()) {
            return List.of(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"));
        } else if (payload.isStoreAdmin()) {
            return List.of(new SimpleGrantedAuthority("ROLE_STORE_ADMIN"));
        } else if (payload.isTableSession()) {
            return List.of(new SimpleGrantedAuthority("ROLE_TABLE_SESSION"));
        }
        return List.of();
    }
}
