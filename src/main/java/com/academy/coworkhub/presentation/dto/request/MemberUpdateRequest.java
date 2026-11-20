package com.academy.coworkhub.presentation.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record MemberUpdateRequest(
        @NotBlank @Size(max = 150) String nombreCompleto,
        @NotBlank @Email @Size(max = 150) String email,
        @NotNull Long planId) {
}
