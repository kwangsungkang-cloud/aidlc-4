package com.tableorder.menu.service;

import com.tableorder.common.exception.BusinessException;
import com.tableorder.common.exception.ErrorCode;
import com.tableorder.menu.dto.*;
import com.tableorder.menu.entity.Category;
import com.tableorder.menu.entity.Menu;
import com.tableorder.menu.repository.CategoryRepository;
import com.tableorder.menu.repository.MenuRepository;
import com.tableorder.storage.service.FileStorageService;
import com.tableorder.store.entity.Store;
import com.tableorder.store.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MenuService {

    private final MenuRepository menuRepository;
    private final CategoryRepository categoryRepository;
    private final StoreRepository storeRepository;
    private final FileStorageService fileStorageService;

    /**
     * 카테고리별 메뉴 조회 (고객용, 캐시 적용)
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "menus", key = "#storeId")
    public CategoryMenuResponse getMenusByStore(Long storeId) {
        List<Category> categories = categoryRepository.findByStoreIdOrderByDisplayOrderAsc(storeId);
        List<Menu> menus = menuRepository.findAllByStoreIdWithCategory(storeId);

        // 카테고리별 메뉴 그룹화
        Map<Long, List<Menu>> menusByCategory = menus.stream()
                .collect(Collectors.groupingBy(
                        m -> m.getCategory().getId(),
                        LinkedHashMap::new,
                        Collectors.toList()));

        List<CategoryMenuResponse.CategoryItem> categoryItems = categories.stream()
                .map(cat -> CategoryMenuResponse.CategoryItem.builder()
                        .categoryId(cat.getId())
                        .name(cat.getName())
                        .menus(menusByCategory.getOrDefault(cat.getId(), List.of()).stream()
                                .map(m -> CategoryMenuResponse.MenuItem.builder()
                                        .menuId(m.getId())
                                        .name(m.getName())
                                        .price(m.getPrice())
                                        .description(m.getDescription())
                                        .imageUrl(m.getImageUrl())
                                        .build())
                                .toList())
                        .build())
                .toList();

        return CategoryMenuResponse.builder()
                .categories(categoryItems)
                .build();
    }

    /**
     * 메뉴 등록
     */
    @Transactional
    @CacheEvict(value = "menus", key = "#storeId")
    public MenuResponse createMenu(Long storeId, CreateMenuRequest request, MultipartFile image) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.STORE_NOT_FOUND));

        Category category = categoryRepository.findByIdAndStoreId(request.getCategoryId(), storeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND));

        // 이미지 업로드
        String imageUrl = null;
        if (image != null && !image.isEmpty()) {
            imageUrl = fileStorageService.upload(image, storeId);
        }

        // displayOrder 결정
        Integer displayOrder = menuRepository.findNextDisplayOrder(storeId, request.getCategoryId());

        Menu menu = Menu.builder()
                .store(store)
                .category(category)
                .name(request.getName())
                .price(request.getPrice())
                .description(request.getDescription())
                .imageUrl(imageUrl)
                .displayOrder(displayOrder)
                .build();

        Menu saved = menuRepository.save(menu);
        log.info("메뉴 등록: storeId={}, menuId={}, name={}", storeId, saved.getId(), saved.getName());

        return MenuResponse.from(saved);
    }

    /**
     * 메뉴 수정
     */
    @Transactional
    @CacheEvict(value = "menus", key = "#storeId")
    public MenuResponse updateMenu(Long storeId, Long menuId, CreateMenuRequest request, MultipartFile image) {
        Menu menu = menuRepository.findByIdAndStoreId(menuId, storeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MENU_NOT_FOUND));

        Category category = categoryRepository.findByIdAndStoreId(request.getCategoryId(), storeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND));

        // 이미지 변경 처리
        String imageUrl = menu.getImageUrl();
        if (image != null && !image.isEmpty()) {
            // 기존 이미지 삭제
            fileStorageService.delete(menu.getImageUrl());
            // 새 이미지 업로드
            imageUrl = fileStorageService.upload(image, storeId);
        }

        menu.update(request.getName(), request.getPrice(), request.getDescription(), category, imageUrl);
        log.info("메뉴 수정: storeId={}, menuId={}", storeId, menuId);

        return MenuResponse.from(menu);
    }

    /**
     * 외부 URL로부터 이미지를 다운로드하여 메뉴 이미지를 업데이트한다.
     */
    @Transactional
    @CacheEvict(value = "menus", key = "#storeId")
    public MenuResponse updateMenuImageFromUrl(Long storeId, Long menuId, String imageUrl) {
        Menu menu = menuRepository.findByIdAndStoreId(menuId, storeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MENU_NOT_FOUND));

        // 기존 이미지 삭제
        fileStorageService.delete(menu.getImageUrl());

        // URL에서 이미지 다운로드 후 S3 업로드
        String s3Url = fileStorageService.uploadFromUrl(imageUrl, storeId);
        menu.updateImageUrl(s3Url);

        log.info("메뉴 이미지 URL 업데이트: storeId={}, menuId={}, imageUrl={}", storeId, menuId, s3Url);
        return MenuResponse.from(menu);
    }


    /**
     * 메뉴 삭제
     */
    @Transactional
    @CacheEvict(value = "menus", key = "#storeId")
    public void deleteMenu(Long storeId, Long menuId) {
        Menu menu = menuRepository.findByIdAndStoreId(menuId, storeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MENU_NOT_FOUND));

        // S3 이미지 삭제
        fileStorageService.delete(menu.getImageUrl());

        menuRepository.delete(menu);
        log.info("메뉴 삭제: storeId={}, menuId={}", storeId, menuId);
    }

    /**
     * 메뉴 노출 순서 변경
     */
    @Transactional
    @CacheEvict(value = "menus", key = "#storeId")
    public int updateMenuOrder(Long storeId, MenuOrderRequest request) {
        List<Long> menuIds = request.getItems().stream()
                .map(MenuOrderRequest.MenuOrderItem::getMenuId)
                .toList();

        List<Menu> menus = menuRepository.findAllByIdInAndStoreId(menuIds, storeId);

        // 모든 메뉴가 해당 매장 소속인지 확인
        if (menus.size() != menuIds.size()) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED, "접근 권한이 없는 메뉴가 포함되어 있습니다");
        }

        Map<Long, Integer> orderMap = request.getItems().stream()
                .collect(Collectors.toMap(
                        MenuOrderRequest.MenuOrderItem::getMenuId,
                        MenuOrderRequest.MenuOrderItem::getDisplayOrder));

        menus.forEach(menu -> menu.updateDisplayOrder(orderMap.get(menu.getId())));

        log.info("메뉴 순서 변경: storeId={}, 변경 수={}", storeId, menus.size());
        return menus.size();
    }
}
