package com.tableorder.menu.dto;

import com.tableorder.menu.entity.Menu;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class MenuResponse {

    private Long menuId;
    private Long categoryId;
    private String name;
    private Integer price;
    private String description;
    private String imageUrl;
    private Integer displayOrder;
    private LocalDateTime createdAt;

    public static MenuResponse from(Menu menu) {
        return MenuResponse.builder()
                .menuId(menu.getId())
                .categoryId(menu.getCategory().getId())
                .name(menu.getName())
                .price(menu.getPrice())
                .description(menu.getDescription())
                .imageUrl(menu.getImageUrl())
                .displayOrder(menu.getDisplayOrder())
                .createdAt(menu.getCreatedAt())
                .build();
    }
}
