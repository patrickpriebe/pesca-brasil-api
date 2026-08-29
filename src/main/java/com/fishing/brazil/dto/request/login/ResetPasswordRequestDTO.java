package com.fishing.brazil.dto.request.login;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ResetPasswordRequestDTO {

    @NotBlank(message = "O e-mail é obrigatório.")
    private String email;

    @NotBlank(message = "O código de segurança é obrigatório.")
    @Pattern(regexp = "[0-9]{6}", message = "O código de segurança tem 6 dígitos.")
    private String code;

    @NotBlank(message = "A nova senha é obrigatória.")
    @Size(min = 8, message = "A senha precisa ter no mínimo 8 caracteres.")
    private String newPassword;
}
