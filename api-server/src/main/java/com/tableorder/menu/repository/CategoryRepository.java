package com.tableorder.menu.repository;

import com.tableorder.menu.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findByStoreIdOrderByDisplayOrderAsc(Long storeId);

    Optional<Category> findByIdAndStoreId(Long id, Long storeId);
}
