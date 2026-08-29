package com.fishing.brazil.dto.request.login;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class VerifyRequestDTO {

    @NotBlank(message = "O e-mail é obrigatório.")
    private String email;

    @NotBlank(message = "O código de verificação é obrigatório.")
    @Pattern(regexp = "[0-9]{6}", message = "O código de verificação tem 6 dígitos.")
    private String code;
}
