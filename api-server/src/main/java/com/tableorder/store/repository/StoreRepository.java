package com.tableorder.store.repository;

import com.tableorder.store.entity.Store;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StoreRepository extends JpaRepository<Store, Long> {

    Optional<Store> findByStoreCode(String storeCode);
}
