package com.tableorder.sse.service;

import com.tableorder.sse.dto.OrderEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
@Slf4j
public class SseService {

    private static final long SSE_TIMEOUT = 30 * 60 * 1000L; // 30분
    private static final int MAX_CONNECTIONS_PER_STORE = 10;

    private final Map<Long, CopyOnWriteArrayList<SseEmitter>> emitterMap = new ConcurrentHashMap<>();

    /**
     * 매장의 SSE 연결을 생성하고 구독한다.
     */
    public SseEmitter subscribe(Long storeId) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);
        String connectionId = UUID.randomUUID().toString();

        CopyOnWriteArrayList<SseEmitter> emitters = emitterMap.computeIfAbsent(
                storeId, k -> new CopyOnWriteArrayList<>());

        // 매장당 최대 연결 수 초과 시 가장 오래된 연결 종료
        while (emitters.size() >= MAX_CONNECTIONS_PER_STORE) {
            SseEmitter oldest = emitters.remove(0);
            oldest.complete();
            log.info("SSE 연결 풀 초과로 기존 연결 종료: storeId={}", storeId);
        }

        emitters.add(emitter);

        // 콜백 등록
        emitter.onCompletion(() -> {
            emitters.remove(emitter);
            log.debug("SSE 연결 완료: storeId={}, connectionId={}", storeId, connectionId);
        });
        emitter.onTimeout(() -> {
            emitters.remove(emitter);
            log.debug("SSE 연결 타임아웃: storeId={}, connectionId={}", storeId, connectionId);
        });
        emitter.onError(e -> {
            emitters.remove(emitter);
            log.warn("SSE 연결 에러: storeId={}, connectionId={}", storeId, connectionId, e);
        });

        // 초기 CONNECTED 이벤트 전송
        OrderEvent connectedEvent = OrderEvent.builder()
                .eventType("CONNECTED")
                .storeId(storeId)
                .connectedAt(LocalDateTime.now())
                .build();
        sendToEmitter(emitter, connectedEvent);

        log.info("SSE 연결 생성: storeId={}, connectionId={}, 현재 연결 수={}",
                storeId, connectionId, emitters.size());

        return emitter;
    }

    /**
     * 특정 매장의 모든 SSE 연결에 주문 이벤트를 발행한다.
     * Person 1(OrderService)이 호출하는 핵심 메서드.
     */
    public void publishOrderEvent(Long storeId, OrderEvent event) {
        CopyOnWriteArrayList<SseEmitter> emitters = emitterMap.get(storeId);
        if (emitters == null || emitters.isEmpty()) {
            log.debug("SSE 구독자 없음, 이벤트 스킵: storeId={}, eventType={}", storeId, event.getEventType());
            return;
        }

        List<SseEmitter> deadEmitters = new java.util.ArrayList<>();

        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name(event.getEventType())
                        .data(event, MediaType.APPLICATION_JSON));
            } catch (IOException e) {
                deadEmitters.add(emitter);
                log.debug("SSE 이벤트 전송 실패 (dead connection): storeId={}", storeId);
            }
        }

        // Dead emitter 정리
        if (!deadEmitters.isEmpty()) {
            emitters.removeAll(deadEmitters);
            log.debug("Dead SSE 연결 정리: storeId={}, 제거={}개", storeId, deadEmitters.size());
        }
    }

    /**
     * 15초 간격 heartbeat 전송 (연결 유지)
     */
    @Scheduled(fixedRate = 15000)
    public void sendHeartbeat() {
        emitterMap.forEach((storeId, emitters) -> {
            List<SseEmitter> deadEmitters = new java.util.ArrayList<>();
            for (SseEmitter emitter : emitters) {
                try {
                    emitter.send(SseEmitter.event().comment("heartbeat"));
                } catch (IOException e) {
                    deadEmitters.add(emitter);
                }
            }
            if (!deadEmitters.isEmpty()) {
                emitters.removeAll(deadEmitters);
            }
        });
    }

    /**
     * 5분 간격 만료된 emitter 정리
     */
    @Scheduled(fixedRate = 300000)
    public void cleanupExpiredEmitters() {
        emitterMap.entrySet().removeIf(entry -> {
            entry.getValue().removeIf(emitter -> {
                try {
                    emitter.send(SseEmitter.event().comment("ping"));
                    return false;
                } catch (IOException e) {
                    return true;
                }
            });
            return entry.getValue().isEmpty();
        });
    }

    private void sendToEmitter(SseEmitter emitter, OrderEvent event) {
        try {
            emitter.send(SseEmitter.event()
                    .name(event.getEventType())
                    .data(event, MediaType.APPLICATION_JSON));
        } catch (IOException e) {
            log.warn("SSE 초기 이벤트 전송 실패", e);
        }
    }
}
