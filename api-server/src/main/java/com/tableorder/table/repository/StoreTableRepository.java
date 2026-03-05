package com.tableorder.table.repository;

import com.tableorder.table.entity.StoreTable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StoreTableRepository extends JpaRepository<StoreTable, Long> {

    Optional<StoreTable> findByStoreIdAndTableNumber(Long storeId, Integer tableNumber);

    List<StoreTable> findByStoreIdOrderByTableNumberAsc(Long storeId);

    boolean existsByStoreIdAndTableNumber(Long storeId, Integer tableNumber);
}
