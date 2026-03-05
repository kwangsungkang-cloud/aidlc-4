package com.tableorder.auth.controller;

import com.tableorder.auth.dto.SuperAdminLoginRequest;
import com.tableorder.auth.dto.SuperAdminLoginResponse;
import com.tableorder.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/super-admin/auth")
@RequiredArgsConstructor
public class SuperAdminAuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<SuperAdminLoginResponse> login(@Valid @RequestBody SuperAdminLoginRequest request) {
        return ResponseEntity.ok(authService.loginSuperAdmin(request));
    }
}
