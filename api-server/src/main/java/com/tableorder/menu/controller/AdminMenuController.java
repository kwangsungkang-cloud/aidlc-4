package com.tableorder.menu.controller;

import com.tableorder.common.security.SecurityContextUtil;
import com.tableorder.menu.dto.CreateMenuRequest;
import com.tableorder.menu.dto.MenuOrderRequest;
import com.tableorder.menu.dto.MenuResponse;
import com.tableorder.menu.service.MenuService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/menus")
@RequiredArgsConstructor
public class AdminMenuController {

    private final MenuService menuService;

    /**
     * 메뉴 등록
     * POST /api/admin/menus
     */
    @PostMapping
    public ResponseEntity<MenuResponse> createMenu(
            @Valid @RequestPart("menu") CreateMenuRequest request,
            @RequestPart(value = "image", required = false) MultipartFile image) {
        Long storeId = SecurityContextUtil.getCurrentStoreId();
        MenuResponse response = menuService.createMenu(storeId, request, image);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 메뉴 수정
     * PUT /api/admin/menus/{menuId}
     */
    @PutMapping("/{menuId}")
    public ResponseEntity<MenuResponse> updateMenu(
            @PathVariable Long menuId,
            @Valid @RequestPart("menu") CreateMenuRequest request,
            @RequestPart(value = "image", required = false) MultipartFile image) {
        Long storeId = SecurityContextUtil.getCurrentStoreId();
        MenuResponse response = menuService.updateMenu(storeId, menuId, request, image);
        return ResponseEntity.ok(response);
    }

    /**
     * 메뉴 삭제
     * DELETE /api/admin/menus/{menuId}
     */
    @DeleteMapping("/{menuId}")
    public ResponseEntity<Map<String, Object>> deleteMenu(@PathVariable Long menuId) {
        Long storeId = SecurityContextUtil.getCurrentStoreId();
        menuService.deleteMenu(storeId, menuId);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "deletedMenuId", menuId));
    }

    /**
     * 메뉴 노출 순서 변경
     * PATCH /api/admin/menus/order
     */
    @PatchMapping("/order")
    public ResponseEntity<Map<String, Object>> updateMenuOrder(
            @Valid @RequestBody MenuOrderRequest request) {
        Long storeId = SecurityContextUtil.getCurrentStoreId();
        int updatedCount = menuService.updateMenuOrder(storeId, request);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "updatedCount", updatedCount));
    }
}
