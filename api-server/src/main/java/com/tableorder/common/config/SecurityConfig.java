package com.tableorder.common.config;

import com.tableorder.common.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> {})
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .headers(headers -> headers
                .contentTypeOptions(contentType -> {})
                .cacheControl(cache -> {})
            )
            .authorizeHttpRequests(auth -> auth
                // Public endpoints
                .requestMatchers("/api/table/auth/**").permitAll()
                .requestMatchers("/api/admin/auth/**").permitAll()
                .requestMatchers("/api/super-admin/auth/**").permitAll()
                .requestMatchers("/actuator/health").permitAll()
                // Customer menu - allow admin access for menu management
                .requestMatchers(HttpMethod.GET, "/api/customer/menus").hasAnyRole("TABLE_SESSION", "STORE_ADMIN")
                // Customer endpoints
                .requestMatchers("/api/customer/**").hasRole("TABLE_SESSION")
                // Admin endpoints
                .requestMatchers("/api/admin/**").hasRole("STORE_ADMIN")
                // Super admin endpoints
                .requestMatchers("/api/super-admin/**").hasRole("SUPER_ADMIN")
                // Default deny
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }
}
