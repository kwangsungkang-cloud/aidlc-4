package com.tableorder.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class AdminLoginRequest {

    @NotBlank(message = "매장 코드는 필수입니다")
    @Size(max = 50, message = "매장 코드는 50자 이하여야 합니다")
    private String storeCode;

    @NotBlank(message = "사용자명은 필수입니다")
    @Size(max = 50, message = "사용자명은 50자 이하여야 합니다")
    private String username;

    @NotBlank(message = "비밀번호는 필수입니다")
    private String password;
}
