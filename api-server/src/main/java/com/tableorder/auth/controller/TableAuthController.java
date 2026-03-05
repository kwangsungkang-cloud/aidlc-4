package com.tableorder.auth.controller;

import com.tableorder.auth.dto.TableLoginRequest;
import com.tableorder.auth.dto.TableLoginResponse;
import com.tableorder.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/table/auth")
@RequiredArgsConstructor
public class TableAuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<TableLoginResponse> login(@Valid @RequestBody TableLoginRequest request) {
        return ResponseEntity.ok(authService.loginTable(request));
    }
}
