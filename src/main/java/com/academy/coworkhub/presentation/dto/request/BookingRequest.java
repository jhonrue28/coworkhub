package com.academy.coworkhub.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDateTime;
import java.util.List;

public record BookingRequest(
        @Schema(description = "Id de la sala", example = "1") @NotNull Long salaId,
        @Schema(description = "Id del miembro a cuyo nombre se reserva (un MEMBER solo puede usar el suyo)", example = "1") @NotNull Long miembroId,
        @Schema(description = "Inicio: futuro, con 1 h de anticipación, dentro del horario de la sede", example = "2030-06-03T10:00:00") @NotNull LocalDateTime inicio,
        @Schema(description = "Fin: el mismo día; duración de 30 min a 8 h en múltiplos de 30 min", example = "2030-06-03T12:00:00") @NotNull LocalDateTime fin,
        @Schema(description = "Asistentes; no superan la capacidad de la sala", example = "4") @NotNull @Positive Integer asistentes,
        @Schema(description = "Servicios adicionales (opcional)") List<@Valid @NotNull BookingItemRequest> servicios) {
}
