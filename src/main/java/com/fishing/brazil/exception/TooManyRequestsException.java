package com.fishing.brazil.exception;

/**
 * O chamador excedeu o limite de requisicoes da janela atual. Vira 429.
 */
public class TooManyRequestsException extends RuntimeException {

    public TooManyRequestsException(String message) {
        super(message);
    }
}
