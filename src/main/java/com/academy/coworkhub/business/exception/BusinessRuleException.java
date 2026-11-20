package com.academy.coworkhub.business.exception;

/** Violación de una regla de negocio (RN-xx) o duplicado: se responde 409 con el código de la regla. */
public class BusinessRuleException extends RuntimeException {

    private final String code;

    public BusinessRuleException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
