package com.tableorder.menu.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CategoryMenuResponse {

    private List<CategoryItem> categories;

    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CategoryItem {
        private Long categoryId;
        private String name;
        private List<MenuItem> menus;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class MenuItem {
        private Long menuId;
        private String name;
        private Integer price;
        private String description;
        private String imageUrl;
    }
}
