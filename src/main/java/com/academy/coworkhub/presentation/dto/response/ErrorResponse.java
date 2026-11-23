package com.academy.coworkhub.presentation.dto.response;

import java.time.Instant;

/**
 * Cuerpo único de todo error de la API.
 * {@code codigo} es la regla violada (RN-xx) o el tipo de error.
 */
public record ErrorResponse(String error, String codigo, Instant timestamp) {

    public static final String INVALID_DATA = "DATOS_INVALIDOS";
    public static final String NOT_FOUND = "NO_ENCONTRADO";
    public static final String UNAUTHORIZED = "NO_AUTENTICADO";
    public static final String FORBIDDEN = "ACCESO_DENEGADO";
    public static final String METHOD_NOT_ALLOWED = "METODO_NO_PERMITIDO";
    public static final String INTERNAL_ERROR = "ERROR_INTERNO";
}
