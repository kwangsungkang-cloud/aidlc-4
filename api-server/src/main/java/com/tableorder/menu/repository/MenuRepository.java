package com.tableorder.menu.repository;

import com.tableorder.menu.entity.Menu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MenuRepository extends JpaRepository<Menu, Long> {

    @Query("SELECT m FROM Menu m JOIN FETCH m.category WHERE m.store.id = :storeId ORDER BY m.category.displayOrder ASC, m.displayOrder ASC")
    List<Menu> findAllByStoreIdWithCategory(@Param("storeId") Long storeId);

    Optional<Menu> findByIdAndStoreId(Long id, Long storeId);

    List<Menu> findAllByIdInAndStoreId(List<Long> ids, Long storeId);

    @Query("SELECT COALESCE(MAX(m.displayOrder), 0) + 1 FROM Menu m WHERE m.store.id = :storeId AND m.category.id = :categoryId")
    Integer findNextDisplayOrder(@Param("storeId") Long storeId, @Param("categoryId") Long categoryId);
}
