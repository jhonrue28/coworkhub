package com.academy.coworkhub.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @Schema(description = "Usuario", example = "admin") @NotBlank String username,
        @Schema(description = "Contraseña", example = "admin123") @NotBlank String password) {
}
