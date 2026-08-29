package com.fishing.brazil.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Setter
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StandardError {
    private LocalDateTime timestamp;
    private Integer status;
    private String error;
    private String message;
    private String path;

    /**
     * Preenchido apenas em erros de validacao: um mapa campo -> mensagem, para que o
     * frontend consiga colocar cada mensagem ao lado do seu input em vez de exibir
     * uma unica frase concatenada.
     */
    private Map<String, String> fieldErrors;

    public StandardError(LocalDateTime timestamp, Integer status, String error, String message, String path) {
        this(timestamp, status, error, message, path, null);
    }
}
