CREATE TABLE table_session (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    table_id BIGINT NOT NULL,
    session_token VARCHAR(255) NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    started_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at DATETIME NULL,
    expires_at DATETIME NOT NULL,
    INDEX idx_table_status (table_id, status),
    INDEX idx_session_token (session_token),
    CONSTRAINT fk_session_table FOREIGN KEY (table_id) REFERENCES store_table(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
