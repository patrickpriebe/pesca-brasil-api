package com.fishing.brazil.dto.request.login;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequestDTO {

    @NotBlank(message = "O nome é obrigatório.")
    @Size(max = 100, message = "O nome deve ter no máximo 100 caracteres.")
    private String name;

    @NotBlank(message = "O e-mail é obrigatório.")
    @Email(message = "Informe um e-mail válido.")
    @Size(max = 100, message = "O e-mail deve ter no máximo 100 caracteres.")
    private String email;

    // O minimo tambem e verificado no UserService, que e onde a regra vale para o
    // reset de senha: a anotacao aqui apenas devolve a mensagem no campo certo.
    @NotBlank(message = "A senha é obrigatória.")
    @Size(min = 8, message = "A senha precisa ter no mínimo 8 caracteres.")
    private String password;

    private String recaptchaToken;
}
