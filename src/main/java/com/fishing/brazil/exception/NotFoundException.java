package com.fishing.brazil.exception;

/**
 * O recurso referenciado nao existe. Vira 404.
 *
 * <p>Tambem e o que responde um recurso que existe mas nao pertence a quem pediu:
 * um 403 confirmaria a existencia do registro para quem estiver testando ids.
 */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
