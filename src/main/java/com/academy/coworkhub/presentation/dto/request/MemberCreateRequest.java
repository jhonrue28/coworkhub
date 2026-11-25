package com.academy.coworkhub.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Registra el miembro y su cuenta de usuario (rol MEMBER) a la vez. */
public record MemberCreateRequest(
        @Schema(description = "Número de documento, único", example = "1010101010") @NotBlank @Size(max = 30) String documento,
        @Schema(description = "Nombre completo", example = "Ana Gómez") @NotBlank @Size(max = 150) String nombreCompleto,
        @Schema(description = "Email, único", example = "ana.gomez@correo.com") @NotBlank @Email @Size(max = 150) String email,
        @Schema(description = "Id del plan de membresía", example = "1") @NotNull Long planId,
        @Schema(description = "Usuario para iniciar sesión, único", example = "ana") @NotBlank @Size(min = 3, max = 50) String usuario,
        @Schema(description = "Contraseña (se guarda con BCrypt)", example = "ana12345") @NotBlank @Size(min = 6, max = 72) String password) {
}
