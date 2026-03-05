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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class MenuServiceTest {

    @InjectMocks
    private MenuService menuService;

    @Mock
    private MenuRepository menuRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private StoreRepository storeRepository;
    @Mock
    private FileStorageService fileStorageService;

    private Store createStore(Long id) {
        return Store.builder()
                .id(id)
                .storeCode("STORE001")
                .storeName("테스트 매장")
                .build();
    }

    private Category createCategory(Long id, Store store) {
        return Category.builder()
                .id(id)
                .store(store)
                .name("메인 메뉴")
                .displayOrder(1)
                .build();
    }

    private Menu createMenu(Long id, Store store, Category category) {
        return Menu.builder()
                .id(id)
                .store(store)
                .category(category)
                .name("김치찌개")
                .price(9000)
                .description("맛있는 김치찌개")
                .imageUrl("https://bucket.s3.amazonaws.com/menus/1/test.jpg")
                .displayOrder(1)
                .build();
    }

    @Nested
    @DisplayName("getMenusByStore - 카테고리별 메뉴 조회")
    class GetMenusByStore {

        @Test
        @DisplayName("매장의 카테고리별 메뉴를 정상 조회한다")
        void 카테고리별_메뉴_정상_조회() {
            // given
            Long storeId = 1L;
            Store store = createStore(storeId);
            Category category = createCategory(1L, store);
            Menu menu = createMenu(1L, store, category);

            given(categoryRepository.findByStoreIdOrderByDisplayOrderAsc(storeId))
                    .willReturn(List.of(category));
            given(menuRepository.findAllByStoreIdWithCategory(storeId))
                    .willReturn(List.of(menu));

            // when
            CategoryMenuResponse response = menuService.getMenusByStore(storeId);

            // then
            assertThat(response.getCategories()).hasSize(1);
            assertThat(response.getCategories().get(0).getName()).isEqualTo("메인 메뉴");
            assertThat(response.getCategories().get(0).getMenus()).hasSize(1);
            assertThat(response.getCategories().get(0).getMenus().get(0).getName()).isEqualTo("김치찌개");
            assertThat(response.getCategories().get(0).getMenus().get(0).getPrice()).isEqualTo(9000);
        }

        @Test
        @DisplayName("메뉴가 없는 카테고리는 빈 리스트로 반환한다")
        void 메뉴_없는_카테고리_빈_리스트() {
            // given
            Long storeId = 1L;
            Store store = createStore(storeId);
            Category category = createCategory(1L, store);

            given(categoryRepository.findByStoreIdOrderByDisplayOrderAsc(storeId))
                    .willReturn(List.of(category));
            given(menuRepository.findAllByStoreIdWithCategory(storeId))
                    .willReturn(List.of());

            // when
            CategoryMenuResponse response = menuService.getMenusByStore(storeId);

            // then
            assertThat(response.getCategories()).hasSize(1);
            assertThat(response.getCategories().get(0).getMenus()).isEmpty();
        }
    }

    @Nested
    @DisplayName("createMenu - 메뉴 등록")
    class CreateMenu {

        @Test
        @DisplayName("이미지 없이 메뉴를 정상 등록한다")
        void 이미지_없이_메뉴_등록() {
            // given
            Long storeId = 1L;
            Store store = createStore(storeId);
            Category category = createCategory(1L, store);

            CreateMenuRequest request = new CreateMenuRequest();
            request.setCategoryId(1L);
            request.setName("된장찌개");
            request.setPrice(8000);
            request.setDescription("구수한 된장찌개");

            given(storeRepository.findById(storeId)).willReturn(Optional.of(store));
            given(categoryRepository.findByIdAndStoreId(1L, storeId)).willReturn(Optional.of(category));
            given(menuRepository.findNextDisplayOrder(storeId, 1L)).willReturn(1);
            given(menuRepository.save(any(Menu.class))).willAnswer(invocation -> {
                Menu m = invocation.getArgument(0);
                return Menu.builder()
                        .id(1L)
                        .store(m.getStore())
                        .category(m.getCategory())
                        .name(m.getName())
                        .price(m.getPrice())
                        .description(m.getDescription())
                        .imageUrl(m.getImageUrl())
                        .displayOrder(m.getDisplayOrder())
                        .build();
            });

            // when
            MenuResponse response = menuService.createMenu(storeId, request, null);

            // then
            assertThat(response.getName()).isEqualTo("된장찌개");
            assertThat(response.getPrice()).isEqualTo(8000);
            assertThat(response.getImageUrl()).isNull();
            then(fileStorageService).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("이미지와 함께 메뉴를 등록한다")
        void 이미지_포함_메뉴_등록() {
            // given
            Long storeId = 1L;
            Store store = createStore(storeId);
            Category category = createCategory(1L, store);
            MultipartFile image = mock(MultipartFile.class);
            given(image.isEmpty()).willReturn(false);

            CreateMenuRequest request = new CreateMenuRequest();
            request.setCategoryId(1L);
            request.setName("비빔밥");
            request.setPrice(10000);

            given(storeRepository.findById(storeId)).willReturn(Optional.of(store));
            given(categoryRepository.findByIdAndStoreId(1L, storeId)).willReturn(Optional.of(category));
            given(menuRepository.findNextDisplayOrder(storeId, 1L)).willReturn(1);
            given(fileStorageService.upload(image, storeId)).willReturn("https://bucket.s3.amazonaws.com/menus/1/img.jpg");
            given(menuRepository.save(any(Menu.class))).willAnswer(invocation -> {
                Menu m = invocation.getArgument(0);
                return Menu.builder()
                        .id(1L)
                        .store(m.getStore())
                        .category(m.getCategory())
                        .name(m.getName())
                        .price(m.getPrice())
                        .description(m.getDescription())
                        .imageUrl(m.getImageUrl())
                        .displayOrder(m.getDisplayOrder())
                        .build();
            });

            // when
            MenuResponse response = menuService.createMenu(storeId, request, image);

            // then
            assertThat(response.getImageUrl()).isEqualTo("https://bucket.s3.amazonaws.com/menus/1/img.jpg");
            then(fileStorageService).should().upload(image, storeId);
        }

        @Test
        @DisplayName("존재하지 않는 매장이면 예외를 던진다")
        void 매장_없으면_예외() {
            // given
            Long storeId = 999L;
            CreateMenuRequest request = new CreateMenuRequest();
            request.setCategoryId(1L);
            request.setName("테스트");
            request.setPrice(1000);

            given(storeRepository.findById(storeId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> menuService.createMenu(storeId, request, null))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.STORE_NOT_FOUND);
        }

        @Test
        @DisplayName("존재하지 않는 카테고리면 예외를 던진다")
        void 카테고리_없으면_예외() {
            // given
            Long storeId = 1L;
            Store store = createStore(storeId);

            CreateMenuRequest request = new CreateMenuRequest();
            request.setCategoryId(999L);
            request.setName("테스트");
            request.setPrice(1000);

            given(storeRepository.findById(storeId)).willReturn(Optional.of(store));
            given(categoryRepository.findByIdAndStoreId(999L, storeId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> menuService.createMenu(storeId, request, null))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.CATEGORY_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("updateMenu - 메뉴 수정")
    class UpdateMenu {

        @Test
        @DisplayName("이미지 변경 없이 메뉴를 수정한다")
        void 이미지_변경_없이_수정() {
            // given
            Long storeId = 1L;
            Long menuId = 1L;
            Store store = createStore(storeId);
            Category category = createCategory(1L, store);
            Menu menu = createMenu(menuId, store, category);

            CreateMenuRequest request = new CreateMenuRequest();
            request.setCategoryId(1L);
            request.setName("김치찌개 (매운맛)");
            request.setPrice(10000);
            request.setDescription("아주 매운 김치찌개");

            given(menuRepository.findByIdAndStoreId(menuId, storeId)).willReturn(Optional.of(menu));
            given(categoryRepository.findByIdAndStoreId(1L, storeId)).willReturn(Optional.of(category));

            // when
            MenuResponse response = menuService.updateMenu(storeId, menuId, request, null);

            // then
            assertThat(response.getName()).isEqualTo("김치찌개 (매운맛)");
            assertThat(response.getPrice()).isEqualTo(10000);
            then(fileStorageService).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("새 이미지로 교체하면 기존 이미지를 삭제한다")
        void 이미지_교체시_기존_삭제() {
            // given
            Long storeId = 1L;
            Long menuId = 1L;
            Store store = createStore(storeId);
            Category category = createCategory(1L, store);
            Menu menu = createMenu(menuId, store, category);
            MultipartFile newImage = mock(MultipartFile.class);
            given(newImage.isEmpty()).willReturn(false);

            CreateMenuRequest request = new CreateMenuRequest();
            request.setCategoryId(1L);
            request.setName("김치찌개");
            request.setPrice(9000);

            given(menuRepository.findByIdAndStoreId(menuId, storeId)).willReturn(Optional.of(menu));
            given(categoryRepository.findByIdAndStoreId(1L, storeId)).willReturn(Optional.of(category));
            given(fileStorageService.upload(newImage, storeId)).willReturn("https://bucket.s3.amazonaws.com/menus/1/new.jpg");

            // when
            menuService.updateMenu(storeId, menuId, request, newImage);

            // then
            then(fileStorageService).should().delete("https://bucket.s3.amazonaws.com/menus/1/test.jpg");
            then(fileStorageService).should().upload(newImage, storeId);
        }

        @Test
        @DisplayName("존재하지 않는 메뉴면 예외를 던진다")
        void 메뉴_없으면_예외() {
            // given
            Long storeId = 1L;
            Long menuId = 999L;
            CreateMenuRequest request = new CreateMenuRequest();
            request.setCategoryId(1L);
            request.setName("테스트");
            request.setPrice(1000);

            given(menuRepository.findByIdAndStoreId(menuId, storeId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> menuService.updateMenu(storeId, menuId, request, null))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.MENU_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("deleteMenu - 메뉴 삭제")
    class DeleteMenu {

        @Test
        @DisplayName("메뉴를 삭제하고 S3 이미지도 삭제한다")
        void 메뉴_삭제_성공() {
            // given
            Long storeId = 1L;
            Long menuId = 1L;
            Store store = createStore(storeId);
            Category category = createCategory(1L, store);
            Menu menu = createMenu(menuId, store, category);

            given(menuRepository.findByIdAndStoreId(menuId, storeId)).willReturn(Optional.of(menu));

            // when
            menuService.deleteMenu(storeId, menuId);

            // then
            then(fileStorageService).should().delete(menu.getImageUrl());
            then(menuRepository).should().delete(menu);
        }

        @Test
        @DisplayName("존재하지 않는 메뉴 삭제 시 예외를 던진다")
        void 메뉴_없으면_예외() {
            // given
            Long storeId = 1L;
            Long menuId = 999L;

            given(menuRepository.findByIdAndStoreId(menuId, storeId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> menuService.deleteMenu(storeId, menuId))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.MENU_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("updateMenuOrder - 메뉴 순서 변경")
    class UpdateMenuOrder {

        @Test
        @DisplayName("메뉴 순서를 정상 변경한다")
        void 순서_변경_성공() {
            // given
            Long storeId = 1L;
            Store store = createStore(storeId);
            Category category = createCategory(1L, store);
            Menu menu1 = createMenu(1L, store, category);
            Menu menu2 = createMenu(2L, store, category);

            MenuOrderRequest request = new MenuOrderRequest();
            MenuOrderRequest.MenuOrderItem item1 = new MenuOrderRequest.MenuOrderItem();
            item1.setMenuId(1L);
            item1.setDisplayOrder(2);
            MenuOrderRequest.MenuOrderItem item2 = new MenuOrderRequest.MenuOrderItem();
            item2.setMenuId(2L);
            item2.setDisplayOrder(1);
            request.setItems(List.of(item1, item2));

            given(menuRepository.findAllByIdInAndStoreId(List.of(1L, 2L), storeId))
                    .willReturn(List.of(menu1, menu2));

            // when
            int result = menuService.updateMenuOrder(storeId, request);

            // then
            assertThat(result).isEqualTo(2);
        }

        @Test
        @DisplayName("다른 매장의 메뉴가 포함되면 예외를 던진다")
        void 다른_매장_메뉴_포함시_예외() {
            // given
            Long storeId = 1L;
            Store store = createStore(storeId);
            Category category = createCategory(1L, store);
            Menu menu1 = createMenu(1L, store, category);

            MenuOrderRequest request = new MenuOrderRequest();
            MenuOrderRequest.MenuOrderItem item1 = new MenuOrderRequest.MenuOrderItem();
            item1.setMenuId(1L);
            item1.setDisplayOrder(1);
            MenuOrderRequest.MenuOrderItem item2 = new MenuOrderRequest.MenuOrderItem();
            item2.setMenuId(999L);
            item2.setDisplayOrder(2);
            request.setItems(List.of(item1, item2));

            // 999번 메뉴는 해당 매장 소속이 아니므로 1개만 반환
            given(menuRepository.findAllByIdInAndStoreId(List.of(1L, 999L), storeId))
                    .willReturn(List.of(menu1));

            // when & then
            assertThatThrownBy(() -> menuService.updateMenuOrder(storeId, request))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.ACCESS_DENIED);
        }
    }
}
