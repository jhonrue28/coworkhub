package com.academy.coworkhub.presentation.dto.request;

import jakarta.validation.constraints.*;

public record MembershipPlanRequest(
        @NotBlank @Size(max = 100) String nombre,
        @NotNull @PositiveOrZero Integer horasIncluidasMes,
        @NotNull @Min(0) @Max(100) Integer descuentoExcedente,
        @NotNull @Positive Integer maxReservasActivas) {
}
