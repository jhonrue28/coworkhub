package com.academy.coworkhub.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record MemberUpdateRequest(
        @Schema(description = "Nombre completo", example = "Ana Gómez") @NotBlank @Size(max = 150) String nombreCompleto,
        @Schema(description = "Email, único", example = "ana.gomez@correo.com") @NotBlank @Email @Size(max = 150) String email,
        @Schema(description = "Id del plan de membresía", example = "2") @NotNull Long planId) {
}
