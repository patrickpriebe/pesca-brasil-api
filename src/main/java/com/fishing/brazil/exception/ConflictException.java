package com.fishing.brazil.exception;

/**
 * O estado atual do recurso impede a operacao. Vira 409.
 *
 * <p>Distinto de BusinessException porque o cliente nao errou: o e-mail ja
 * cadastrado ou a conta ja verificada sao respostas sobre o servidor, e nao
 * sobre a requisicao.
 */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
