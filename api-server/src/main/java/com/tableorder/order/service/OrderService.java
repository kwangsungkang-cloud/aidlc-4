package com.tableorder.order.service;

import com.tableorder.common.exception.BusinessException;
import com.tableorder.common.exception.ErrorCode;
import com.tableorder.order.dto.*;
import com.tableorder.order.entity.Order;
import com.tableorder.order.entity.OrderItem;
import com.tableorder.order.repository.OrderRepository;
import com.tableorder.table.entity.StoreTable;
import com.tableorder.table.entity.TableSession;
import com.tableorder.table.repository.StoreTableRepository;
import com.tableorder.table.repository.TableSessionRepository;
import com.tableorder.table.service.TableSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final TableSessionService tableSessionService;
    private final TableSessionRepository tableSessionRepository;
    private final StoreTableRepository storeTableRepository;
    private final MenuValidator menuValidator;

    // --- 고객: 주문 생성 ---
    @Transactional
    public CreateOrderResponse createOrder(Long sessionId, Long storeId, CreateOrderRequest request) {
        TableSession session = tableSessionService.getActiveSession(sessionId);

        // 메뉴 검증 및 스냅샷 조회
        List<MenuValidator.MenuSnapshot> snapshots = menuValidator.validateAndSnapshot(
                request.getItems(), storeId);

        // 주문 번호 생성
        String orderNumber = generateOrderNumber(storeId);

        // OrderItem 생성 및 총액 계산
        int totalAmount = 0;
        List<OrderItem> items = new ArrayList<>();
        for (int i = 0; i < request.getItems().size(); i++) {
            CreateOrderRequest.OrderItemRequest itemReq = request.getItems().get(i);
            MenuValidator.MenuSnapshot snapshot = snapshots.get(i);
            int subtotal = itemReq.getQuantity() * snapshot.price();

            items.add(OrderItem.builder()
                    .menuId(itemReq.getMenuId())
                    .menuName(snapshot.name())
                    .quantity(itemReq.getQuantity())
                    .unitPrice(snapshot.price())
                    .subtotal(subtotal)
                    .build());
            totalAmount += subtotal;
        }

        // Order 엔티티 생성
        Order order = Order.builder()
                .session(session)
                .orderNumber(orderNumber)
                .status(Order.Status.PENDING)
                .totalAmount(totalAmount)
                .items(new ArrayList<>())
                .build();

        for (OrderItem item : items) {
            order.addItem(item);
        }

        order = orderRepository.save(order);

        log.info("Order created: orderNumber={}, sessionId={}, totalAmount={}",
                orderNumber, sessionId, totalAmount);

        // SSE 이벤트 발행은 Person 2의 SseService가 준비되면 연동
        // sseService.publishOrderEvent(storeId, buildNewOrderEvent(order));

        return CreateOrderResponse.from(order);
    }

    // --- 고객: 현재 세션 주문 조회 ---
    @Transactional(readOnly = true)
    public OrderListResponse getOrdersBySession(Long sessionId) {
        tableSessionService.getActiveSession(sessionId);
        List<Order> orders = orderRepository.findBySessionIdWithItems(sessionId);
        return OrderListResponse.from(sessionId, orders);
    }

    // --- 관리자: 주문 상태 변경 ---
    @Transactional
    public OrderStatusResponse changeOrderStatus(Long orderId, Long storeId, OrderStatusRequest request) {
        Order.Status newStatus;
        try {
            newStatus = Order.Status.valueOf(request.getStatus());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_ORDER_STATUS);
        }

        Order order = orderRepository.findByIdWithSessionAndTable(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        Long orderStoreId = order.getSession().getStoreTable().getStore().getId();
        if (!orderStoreId.equals(storeId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        Order.Status previousStatus = order.getStatus();
        if (!previousStatus.canTransitionTo(newStatus)) {
            throw new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION,
                    previousStatus.getTransitionErrorMessage(newStatus));
        }

        order.changeStatus(newStatus);
        orderRepository.save(order);

        log.info("Order status changed: orderId={}, {} -> {}", orderId, previousStatus, newStatus);

        // SSE 이벤트 발행 (Person 2 연동 대기)

        return OrderStatusResponse.builder()
                .orderId(order.getId())
                .orderNumber(order.getOrderNumber())
                .previousStatus(previousStatus.name())
                .status(newStatus.name())
                .updatedAt(order.getUpdatedAt())
                .build();
    }

    // --- 관리자: 주문 삭제 ---
    @Transactional
    public void deleteOrder(Long orderId, Long storeId) {
        Order order = orderRepository.findByIdWithSessionAndTable(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        Long orderStoreId = order.getSession().getStoreTable().getStore().getId();
        if (!orderStoreId.equals(storeId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        log.info("Order deleted: orderId={}, orderNumber={}", orderId, order.getOrderNumber());

        orderRepository.delete(order);

        // SSE 이벤트 발행 (Person 2 연동 대기)
    }

    // --- 관리자: 대시보드 ---
    @Transactional(readOnly = true)
    public DashboardResponse getDashboard(Long storeId) {
        List<StoreTable> tables = storeTableRepository.findByStoreIdOrderByTableNumberAsc(storeId);
        List<Order> activeOrders = orderRepository.findActiveOrdersByStoreId(storeId);

        // 세션별 주문 그룹화
        Map<Long, List<Order>> ordersBySession = activeOrders.stream()
                .collect(Collectors.groupingBy(o -> o.getSession().getId()));

        List<DashboardResponse.TableSummary> tableSummaries = tables.stream()
                .map(table -> {
                    var session = tableSessionRepository
                            .findByStoreTableIdAndStatus(table.getId(), TableSession.Status.ACTIVE)
                            .orElse(null);

                    if (session == null) {
                        return DashboardResponse.TableSummary.builder()
                                .tableId(table.getId())
                                .tableNumber(table.getTableNumber())
                                .sessionId(null)
                                .sessionStatus(null)
                                .totalOrderAmount(0)
                                .orderCount(0)
                                .recentOrders(List.of())
                                .build();
                    }

                    List<Order> sessionOrders = ordersBySession.getOrDefault(session.getId(), List.of());
                    int totalAmount = sessionOrders.stream().mapToInt(Order::getTotalAmount).sum();

                    List<DashboardResponse.RecentOrder> recentOrders = sessionOrders.stream()
                            .sorted(Comparator.comparing(Order::getCreatedAt).reversed())
                            .limit(3)
                            .map(o -> DashboardResponse.RecentOrder.builder()
                                    .orderId(o.getId())
                                    .orderNumber(o.getOrderNumber())
                                    .status(o.getStatus().name())
                                    .totalAmount(o.getTotalAmount())
                                    .itemSummary(buildItemSummary(o))
                                    .createdAt(o.getCreatedAt())
                                    .build())
                            .toList();

                    return DashboardResponse.TableSummary.builder()
                            .tableId(table.getId())
                            .tableNumber(table.getTableNumber())
                            .sessionId(session.getId())
                            .sessionStatus("ACTIVE")
                            .totalOrderAmount(totalAmount)
                            .orderCount(sessionOrders.size())
                            .recentOrders(recentOrders)
                            .build();
                })
                .toList();

        return DashboardResponse.builder()
                .storeId(storeId)
                .tables(tableSummaries)
                .build();
    }

    // --- 관리자: 과거 주문 내역 ---
    @Transactional(readOnly = true)
    public OrderHistoryResponse getOrderHistory(Long tableId, Long storeId,
                                                 LocalDate startDate, LocalDate endDate) {
        StoreTable table = storeTableRepository.findById(tableId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TABLE_NOT_FOUND));

        if (!table.getStore().getId().equals(storeId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        if (startDate == null) startDate = LocalDate.now().minusDays(7);
        if (endDate == null) endDate = LocalDate.now();

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay();

        List<Order> orders = orderRepository.findHistoryByTableIdAndDateRange(
                tableId, startDateTime, endDateTime);

        // 세션별 그룹화
        Map<Long, List<Order>> ordersBySession = orders.stream()
                .collect(Collectors.groupingBy(o -> o.getSession().getId(),
                        LinkedHashMap::new, Collectors.toList()));

        List<OrderHistoryResponse.SessionHistory> sessions = ordersBySession.entrySet().stream()
                .map(entry -> {
                    TableSession session = entry.getValue().get(0).getSession();
                    List<OrderHistoryResponse.OrderDetail> orderDetails = entry.getValue().stream()
                            .map(o -> OrderHistoryResponse.OrderDetail.builder()
                                    .orderId(o.getId())
                                    .orderNumber(o.getOrderNumber())
                                    .totalAmount(o.getTotalAmount())
                                    .items(o.getItems().stream()
                                            .map(item -> OrderHistoryResponse.ItemDetail.builder()
                                                    .menuName(item.getMenuName())
                                                    .quantity(item.getQuantity())
                                                    .unitPrice(item.getUnitPrice())
                                                    .subtotal(item.getSubtotal())
                                                    .build())
                                            .toList())
                                    .createdAt(o.getCreatedAt())
                                    .build())
                            .toList();

                    int sessionTotal = entry.getValue().stream().mapToInt(Order::getTotalAmount).sum();

                    return OrderHistoryResponse.SessionHistory.builder()
                            .sessionId(session.getId())
                            .startedAt(session.getStartedAt())
                            .completedAt(session.getCompletedAt())
                            .totalAmount(sessionTotal)
                            .orders(orderDetails)
                            .build();
                })
                .toList();

        return OrderHistoryResponse.builder()
                .tableId(tableId)
                .tableNumber(table.getTableNumber())
                .sessions(sessions)
                .build();
    }

    // --- 내부: 주문 번호 생성 ---
    private String generateOrderNumber(Long storeId) {
        LocalDate today = LocalDate.now();
        long count = orderRepository.countByStoreIdAndDate(storeId, today);
        String dateStr = today.format(DateTimeFormatter.BASIC_ISO_DATE);
        return String.format("ORD-%s-%04d", dateStr, count + 1);
    }

    // --- 내부: 아이템 요약 문자열 ---
    private String buildItemSummary(Order order) {
        return order.getItems().stream()
                .map(item -> item.getMenuName() + " x" + item.getQuantity())
                .collect(Collectors.joining(", "));
    }
}
