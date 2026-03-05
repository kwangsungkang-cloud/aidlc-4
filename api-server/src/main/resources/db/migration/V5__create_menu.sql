-- Person 2 소유: 메뉴 테이블
CREATE TABLE menu (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    store_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    price INT NOT NULL,
    description VARCHAR(500) NULL,
    image_url VARCHAR(500) NULL,
    display_order INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_store_category (store_id, category_id),
    INDEX idx_category_order (category_id, display_order),
    CONSTRAINT fk_menu_store FOREIGN KEY (store_id) REFERENCES store(id),
    CONSTRAINT fk_menu_category FOREIGN KEY (category_id) REFERENCES category(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
