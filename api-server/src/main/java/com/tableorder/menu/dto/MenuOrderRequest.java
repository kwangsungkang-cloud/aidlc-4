package com.tableorder.menu.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class MenuOrderRequest {

    @NotEmpty(message = "메뉴 순서 항목이 비어있습니다")
    @Valid
    private List<MenuOrderItem> items;

    @Getter
    @Setter
    public static class MenuOrderItem {

        @NotNull(message = "메뉴 ID는 필수입니다")
        private Long menuId;

        @NotNull(message = "순서는 필수입니다")
        private Integer displayOrder;
    }
}
