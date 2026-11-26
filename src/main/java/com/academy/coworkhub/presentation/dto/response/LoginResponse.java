package com.academy.coworkhub.presentation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

public record LoginResponse(
        @Schema(description = "JWT para el encabezado Authorization: Bearer <token>",
                example = "eyJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJjb3dvcmtodWIiLCJzdWIiOiJhZG1pbiIsInJvbGUiOiJBRE1JTiJ9.firma") String token,
        @Schema(description = "Tipo de token", example = "Bearer") String tipo,
        @Schema(description = "Vencimiento del token (UTC)", example = "2030-06-03T14:00:00Z") Instant expiraEn) {
}
