package com.tableorder.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class TableLoginRequest {

    @NotBlank(message = "매장 코드는 필수입니다")
    @Size(max = 50, message = "매장 코드는 50자 이하여야 합니다")
    private String storeCode;

    @NotNull(message = "테이블 번호는 필수입니다")
    @Positive(message = "테이블 번호는 양수여야 합니다")
    private Integer tableNumber;

    @NotBlank(message = "비밀번호는 필수입니다")
    private String password;
}
