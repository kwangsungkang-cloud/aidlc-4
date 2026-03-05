package com.tableorder.order.repository;

import com.tableorder.order.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    @Query("SELECT o FROM Order o JOIN FETCH o.items WHERE o.session.id = :sessionId ORDER BY o.createdAt ASC")
    List<Order> findBySessionIdWithItems(@Param("sessionId") Long sessionId);

    @Query("SELECT o FROM Order o " +
           "JOIN FETCH o.session s " +
           "JOIN FETCH s.storeTable st " +
           "WHERE o.id = :orderId")
    Optional<Order> findByIdWithSessionAndTable(@Param("orderId") Long orderId);

    @Query("SELECT COUNT(o) FROM Order o " +
           "JOIN o.session s " +
           "JOIN s.storeTable st " +
           "WHERE st.store.id = :storeId AND FUNCTION('DATE', o.createdAt) = :date")
    long countByStoreIdAndDate(@Param("storeId") Long storeId, @Param("date") LocalDate date);

    @Modifying
    @Query("UPDATE Order o SET o.status = 'COMPLETED', o.updatedAt = CURRENT_TIMESTAMP " +
           "WHERE o.session.id = :sessionId AND o.status <> 'COMPLETED'")
    int completeAllBySessionId(@Param("sessionId") Long sessionId);

    @Query("SELECT o FROM Order o " +
           "JOIN o.session s " +
           "JOIN s.storeTable st " +
           "WHERE st.store.id = :storeId AND s.status = 'ACTIVE' " +
           "ORDER BY o.createdAt DESC")
    List<Order> findActiveOrdersByStoreId(@Param("storeId") Long storeId);

    @Query("SELECT o FROM Order o " +
           "JOIN FETCH o.items " +
           "JOIN o.session s " +
           "WHERE s.storeTable.id = :tableId AND s.status = 'COMPLETED' " +
           "AND s.completedAt >= :startDate AND s.completedAt < :endDate " +
           "ORDER BY s.completedAt DESC, o.createdAt ASC")
    List<Order> findHistoryByTableIdAndDateRange(
            @Param("tableId") Long tableId,
            @Param("startDate") java.time.LocalDateTime startDate,
            @Param("endDate") java.time.LocalDateTime endDate);
}
