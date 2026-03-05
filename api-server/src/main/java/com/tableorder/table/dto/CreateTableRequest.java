package com.tableorder.table.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CreateTableRequest {

    @NotNull(message = "테이블 번호는 필수입니다")
    @Positive(message = "테이블 번호는 양수여야 합니다")
    private Integer tableNumber;

    @NotBlank(message = "비밀번호는 필수입니다")
    @Size(min = 4, message = "비밀번호는 최소 4자 이상이어야 합니다")
    private String password;
}
