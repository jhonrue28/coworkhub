package com.academy.coworkhub.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

public record MembershipPlanRequest(
        @Schema(description = "Nombre único del plan", example = "Profesional") @NotBlank @Size(max = 100) String nombre,
        @Schema(description = "Horas de sala incluidas cada mes, sin costo", example = "30") @NotNull @PositiveOrZero Integer horasIncluidasMes,
        @Schema(description = "Descuento (%) sobre la tarifa de las horas excedentes", example = "20") @NotNull @Min(0) @Max(100) Integer descuentoExcedente,
        @Schema(description = "Máximo de reservas PENDING o CONFIRMED futuras a la vez", example = "5") @NotNull @Positive Integer maxReservasActivas) {
}
