package com.fishing.brazil.dto.request.login;

import lombok.Data;

@Data
public class LoginRequestDTO {
    private String email;
    private String password;
    private String recaptchaToken;
}