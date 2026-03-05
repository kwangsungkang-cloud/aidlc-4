package com.tableorder.order.controller;

import com.tableorder.common.security.SecurityContextUtil;
import com.tableorder.order.dto.*;
import com.tableorder.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminOrderController {

    private final OrderService orderService;

    @PatchMapping("/orders/{orderId}/status")
    public ResponseEntity<OrderStatusResponse> changeStatus(
            @PathVariable Long orderId,
            @Valid @RequestBody OrderStatusRequest request) {
        Long storeId = SecurityContextUtil.getCurrentStoreId();
        return ResponseEntity.ok(orderService.changeOrderStatus(orderId, storeId, request));
    }

    @DeleteMapping("/orders/{orderId}")
    public ResponseEntity<Map<String, Object>> deleteOrder(@PathVariable Long orderId) {
        Long storeId = SecurityContextUtil.getCurrentStoreId();
        orderService.deleteOrder(orderId, storeId);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "deletedOrderId", orderId
        ));
    }

    @GetMapping("/orders/dashboard")
    public ResponseEntity<DashboardResponse> getDashboard() {
        Long storeId = SecurityContextUtil.getCurrentStoreId();
        return ResponseEntity.ok(orderService.getDashboard(storeId));
    }

    @GetMapping("/tables/{tableId}/history")
    public ResponseEntity<OrderHistoryResponse> getOrderHistory(
            @PathVariable Long tableId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        Long storeId = SecurityContextUtil.getCurrentStoreId();
        return ResponseEntity.ok(orderService.getOrderHistory(tableId, storeId, startDate, endDate));
    }
}
