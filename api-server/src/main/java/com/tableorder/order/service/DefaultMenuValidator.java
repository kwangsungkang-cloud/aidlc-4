package com.tableorder.order.service;

import com.tableorder.common.exception.BusinessException;
import com.tableorder.common.exception.ErrorCode;
import com.tableorder.order.dto.CreateOrderRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * MenuValidator 기본 구현체.
 * Person 2가 menu 패키지에 더 나은 구현체를 제공하면 자동으로 대체됩니다.
 */
@Slf4j
@Component
@ConditionalOnMissingBean(name = "menuMenuValidator")
public class DefaultMenuValidator implements MenuValidator {

    private final JdbcTemplate jdbcTemplate;

    public DefaultMenuValidator(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<MenuSnapshot> validateAndSnapshot(
            List<CreateOrderRequest.OrderItemRequest> items, Long storeId) {

        List<MenuSnapshot> snapshots = new ArrayList<>();

        for (CreateOrderRequest.OrderItemRequest item : items) {
            List<MenuSnapshot> result = jdbcTemplate.query(
                    "SELECT id, name, price, store_id FROM menu WHERE id = ?",
                    (rs, rowNum) -> new MenuSnapshot(
                            rs.getLong("id"),
                            rs.getString("name"),
                            rs.getInt("price")
                    ),
                    item.getMenuId()
            );

            if (result.isEmpty()) {
                throw new BusinessException(ErrorCode.MENU_NOT_FOUND,
                        "메뉴를 찾을 수 없습니다: menuId=" + item.getMenuId());
            }

            // storeId 검증
            Long menuStoreId = jdbcTemplate.queryForObject(
                    "SELECT store_id FROM menu WHERE id = ?", Long.class, item.getMenuId());
            if (!storeId.equals(menuStoreId)) {
                throw new BusinessException(ErrorCode.MENU_STORE_MISMATCH);
            }

            snapshots.add(result.get(0));
        }

        return snapshots;
    }
}
