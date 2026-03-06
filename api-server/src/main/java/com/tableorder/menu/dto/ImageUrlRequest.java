package com.tableorder.menu.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ImageUrlRequest {
    @NotBlank(message = "이미지 URL은 필수입니다")
    private String imageUrl;
}
