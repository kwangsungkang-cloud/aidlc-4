package com.tableorder.menu.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateMenuRequest {

    @NotNull(message = "카테고리 ID는 필수입니다")
    private Long categoryId;

    @NotBlank(message = "메뉴명은 필수입니다")
    @Size(max = 100, message = "메뉴명은 100자 이하여야 합니다")
    private String name;

    @NotNull(message = "가격은 필수입니다")
    @Min(value = 1, message = "가격은 1원 이상이어야 합니다")
    @Max(value = 10000000, message = "가격은 10,000,000원 이하여야 합니다")
    private Integer price;

    @Size(max = 500, message = "설명은 500자 이하여야 합니다")
    private String description;
}
