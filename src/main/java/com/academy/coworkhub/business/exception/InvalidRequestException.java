package com.academy.coworkhub.business.exception;

/** Pedido mal formado que @Valid no puede detectar (por ejemplo, una combinación de filtros): se responde 400. */
public class InvalidRequestException extends RuntimeException {

    public InvalidRequestException(String message) {
        super(message);
    }
}
