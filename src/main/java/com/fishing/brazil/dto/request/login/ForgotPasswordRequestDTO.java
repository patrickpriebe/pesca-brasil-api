package com.fishing.brazil.dto.request.login;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ForgotPasswordRequestDTO {

    @NotBlank(message = "O e-mail é obrigatório.")
    private String email;

    private String recaptchaToken;
}
