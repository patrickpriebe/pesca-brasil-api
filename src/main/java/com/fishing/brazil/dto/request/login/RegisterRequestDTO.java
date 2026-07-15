package com.fishing.brazil.dto.request.login;

import lombok.Data;

@Data
public class RegisterRequestDTO {
    private String name;
    private String email;
    private String password;
    private String recaptchaToken;
}