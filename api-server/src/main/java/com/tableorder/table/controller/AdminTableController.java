package com.tableorder.table.controller;

import com.tableorder.common.security.SecurityContextUtil;
import com.tableorder.table.dto.CreateTableRequest;
import com.tableorder.table.dto.CreateTableResponse;
import com.tableorder.table.dto.EndSessionResponse;
import com.tableorder.table.service.TableSessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/tables")
@RequiredArgsConstructor
public class AdminTableController {

    private final TableSessionService tableSessionService;

    @PostMapping
    public ResponseEntity<CreateTableResponse> createTable(@Valid @RequestBody CreateTableRequest request) {
        Long storeId = SecurityContextUtil.getCurrentStoreId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(tableSessionService.createTable(storeId, request));
    }

    @PostMapping("/{tableId}/end-session")
    public ResponseEntity<EndSessionResponse> endSession(@PathVariable Long tableId) {
        Long storeId = SecurityContextUtil.getCurrentStoreId();
        return ResponseEntity.ok(tableSessionService.endSession(tableId, storeId));
    }
}
