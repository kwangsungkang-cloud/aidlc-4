-- Person 2 소유: 주문 항목 테이블
CREATE TABLE order_item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    menu_id BIGINT NOT NULL,
    menu_name VARCHAR(100) NOT NULL,
    quantity INT NOT NULL,
    unit_price INT NOT NULL,
    subtotal INT NOT NULL,
    INDEX idx_order_id (order_id),
    CONSTRAINT fk_item_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    CONSTRAINT fk_item_menu FOREIGN KEY (menu_id) REFERENCES menu(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
