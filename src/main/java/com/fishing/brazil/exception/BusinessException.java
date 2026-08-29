package com.fishing.brazil.exception;

/**
 * A requisicao esta bem formada, mas viola uma regra de negocio. Vira 400.
 */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }
}
