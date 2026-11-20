package com.academy.coworkhub.presentation.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Registra el miembro y su cuenta de usuario (rol MEMBER) a la vez. */
public record MemberCreateRequest(
        @NotBlank @Size(max = 30) String documento,
        @NotBlank @Size(max = 150) String nombreCompleto,
        @NotBlank @Email @Size(max = 150) String email,
        @NotNull Long planId,
        @NotBlank @Size(min = 3, max = 50) String usuario,
        @NotBlank @Size(min = 6, max = 72) String password) {
}
