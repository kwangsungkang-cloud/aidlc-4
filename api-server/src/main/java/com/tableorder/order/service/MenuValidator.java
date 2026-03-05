package com.tableorder.order.service;

import com.tableorder.order.dto.CreateOrderRequest;

import java.util.List;

/**
 * 메뉴 검증 인터페이스.
 * Person 2(menu 패키지)가 구현체를 제공합니다.
 * order 패키지는 이 인터페이스에만 의존합니다.
 */
public interface MenuValidator {

    /**
     * 주문 항목의 메뉴를 검증하고 스냅샷 정보를 반환합니다.
     *
     * @param items   주문 항목 목록
     * @param storeId 매장 ID (메뉴 소속 검증용)
     * @return 메뉴 스냅샷 목록 (items와 동일 순서)
     * @throws com.tableorder.common.exception.BusinessException 메뉴 없음 또는 매장 불일치 시
     */
    List<MenuSnapshot> validateAndSnapshot(List<CreateOrderRequest.OrderItemRequest> items, Long storeId);

    record MenuSnapshot(Long menuId, String name, int price) {}
}
