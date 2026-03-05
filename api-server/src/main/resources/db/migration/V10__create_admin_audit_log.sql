-- Person 4 소유: 관리자 활동 이력 테이블 (append-only)
CREATE TABLE admin_audit_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    performed_by BIGINT NOT NULL,
    target_admin_id BIGINT NULL,
    target_username VARCHAR(50) NOT NULL,
    store_id BIGINT NOT NULL,
    action_type VARCHAR(20) NOT NULL,
    performed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_performed_at (performed_at),
    INDEX idx_action_type (action_type),
    INDEX idx_store_id (store_id),
    CONSTRAINT fk_audit_performer FOREIGN KEY (performed_by) REFERENCES super_admin(id),
    CONSTRAINT fk_audit_store FOREIGN KEY (store_id) REFERENCES store(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
