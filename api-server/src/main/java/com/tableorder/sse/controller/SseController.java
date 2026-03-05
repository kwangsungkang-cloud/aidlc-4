package com.tableorder.sse.controller;

import com.tableorder.common.security.SecurityContextUtil;
import com.tableorder.sse.service.SseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/admin/sse")
@RequiredArgsConstructor
public class SseController {

    private final SseService sseService;

    /**
     * SSE 구독 엔드포인트 (관리자용)
     * GET /api/admin/sse/subscribe
     */
    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe() {
        Long storeId = SecurityContextUtil.getCurrentStoreId();
        return sseService.subscribe(storeId);
    }
}
