CREATE TABLE store_table (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    store_id BIGINT NOT NULL,
    table_number INT NOT NULL,
    password VARCHAR(255) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_store_table (store_id, table_number),
    INDEX idx_store_id (store_id),
    CONSTRAINT fk_store_table_store FOREIGN KEY (store_id) REFERENCES store(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
