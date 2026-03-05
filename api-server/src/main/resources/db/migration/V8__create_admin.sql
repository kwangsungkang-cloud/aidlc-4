-- Person 4 소유: 매장 관리자 테이블
CREATE TABLE admin (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    store_id BIGINT NOT NULL,
    username VARCHAR(50) NOT NULL,
    password VARCHAR(255) NOT NULL,
    login_attempts INT NOT NULL DEFAULT 0,
    locked_until DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_admin_store_username (store_id, username),
    INDEX idx_store_id (store_id),
    CONSTRAINT fk_admin_store FOREIGN KEY (store_id) REFERENCES store(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
