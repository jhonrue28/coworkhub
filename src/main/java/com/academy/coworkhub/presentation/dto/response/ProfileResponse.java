package com.academy.coworkhub.presentation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record ProfileResponse(
        @Schema(description = "Usuario", example = "ana") String username,
        @Schema(description = "Rol", example = "MEMBER") String role,
        @Schema(description = "Id del miembro; null para ADMIN y RECEPTIONIST", example = "1") Long miembroId) {
}
