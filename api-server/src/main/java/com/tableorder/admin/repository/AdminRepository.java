package com.tableorder.admin.repository;

import com.tableorder.admin.entity.Admin;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AdminRepository extends JpaRepository<Admin, Long> {

    Optional<Admin> findByStoreIdAndUsername(Long storeId, String username);

    List<Admin> findByStoreIdOrderByCreatedAtAsc(Long storeId);

    boolean existsByStoreIdAndUsername(Long storeId, String username);
}
