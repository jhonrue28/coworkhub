package com.academy.coworkhub.business.exception;

/** El recurso pedido no existe: se responde 404. */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String resource, Long id) {
        super(resource + " con id " + id + " no existe");
    }
}
