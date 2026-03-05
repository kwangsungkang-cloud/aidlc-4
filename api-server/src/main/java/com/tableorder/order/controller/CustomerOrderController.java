package com.tableorder.order.controller;

import com.tableorder.common.security.SecurityContextUtil;
import com.tableorder.order.dto.CreateOrderRequest;
import com.tableorder.order.dto.CreateOrderResponse;
import com.tableorder.order.dto.OrderListResponse;
import com.tableorder.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/customer/orders")
@RequiredArgsConstructor
public class CustomerOrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<CreateOrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        Long sessionId = SecurityContextUtil.getCurrentSessionId();
        Long storeId = SecurityContextUtil.getCurrentStoreId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(orderService.createOrder(sessionId, storeId, request));
    }

    @GetMapping
    public ResponseEntity<OrderListResponse> getOrders() {
        Long sessionId = SecurityContextUtil.getCurrentSessionId();
        return ResponseEntity.ok(orderService.getOrdersBySession(sessionId));
    }
}
