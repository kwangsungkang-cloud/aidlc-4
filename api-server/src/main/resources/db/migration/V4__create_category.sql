-- Person 2 소유: 메뉴 카테고리 테이블
CREATE TABLE category (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    store_id BIGINT NOT NULL,
    name VARCHAR(50) NOT NULL,
    display_order INT NOT NULL DEFAULT 0,
    INDEX idx_store_order (store_id, display_order),
    CONSTRAINT fk_category_store FOREIGN KEY (store_id) REFERENCES store(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
