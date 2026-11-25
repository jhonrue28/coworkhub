package com.academy.coworkhub.presentation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/**
 * Cuerpo único de todo error de la API.
 * {@code codigo} es la regla violada (RN-xx) o el tipo de error.
 */
@Schema(description = "Cuerpo único de todo error de la API")
public record ErrorResponse(
        @Schema(description = "Descripción del error", example = "La sala ya está reservada en ese horario") String error,
        @Schema(description = "Regla violada (RN-xx) o tipo de error", example = "RN-01") String codigo,
        @Schema(description = "Momento del error (UTC)", example = "2030-06-03T13:00:00Z") Instant timestamp) {

    public static final String INVALID_DATA = "DATOS_INVALIDOS";
    public static final String NOT_FOUND = "NO_ENCONTRADO";
    public static final String UNAUTHORIZED = "NO_AUTENTICADO";
    public static final String FORBIDDEN = "ACCESO_DENEGADO";
    public static final String METHOD_NOT_ALLOWED = "METODO_NO_PERMITIDO";
    public static final String INTERNAL_ERROR = "ERROR_INTERNO";
}
