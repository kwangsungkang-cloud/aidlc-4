package com.tableorder.menu.controller;

import com.tableorder.common.security.SecurityContextUtil;
import com.tableorder.menu.dto.CategoryMenuResponse;
import com.tableorder.menu.service.MenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/customer/menus")
@RequiredArgsConstructor
public class CustomerMenuController {

    private final MenuService menuService;

    /**
     * 카테고리별 메뉴 조회 (고객용)
     * GET /api/customer/menus
     */
    @GetMapping
    public ResponseEntity<CategoryMenuResponse> getMenus() {
        Long storeId = SecurityContextUtil.getCurrentStoreId();
        CategoryMenuResponse response = menuService.getMenusByStore(storeId);
        return ResponseEntity.ok(response);
    }
}
