package com.tableorder.admin.repository;

import com.tableorder.admin.entity.SuperAdmin;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SuperAdminRepository extends JpaRepository<SuperAdmin, Long> {

    Optional<SuperAdmin> findByUsername(String username);
}
