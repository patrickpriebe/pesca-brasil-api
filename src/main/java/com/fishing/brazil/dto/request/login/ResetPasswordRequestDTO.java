package com.fishing.brazil.dto.request.login;

import lombok.Data;

@Data
public class ResetPasswordRequestDTO {
    private String email;
    private String code;
    private String newPassword;
}