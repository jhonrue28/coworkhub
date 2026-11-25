package com.academy.coworkhub.presentation.dto.request;

import com.academy.coworkhub.persistence.entity.RoomType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.Set;

public record RoomRequest(
        @Schema(description = "Nombre de la sala, único dentro de la sede", example = "Sala Andes") @NotBlank @Size(max = 100) String nombre,
        @Schema(description = "Tipo de sala", example = "MEETING_ROOM") @NotNull RoomType tipo,
        @Schema(description = "Máximo de asistentes", example = "8") @NotNull @Positive Integer capacidad,
        @Schema(description = "Tarifa por hora", example = "50000.00") @NotNull @Positive @Digits(integer = 8, fraction = 2) BigDecimal tarifaHora,
        @Schema(description = "Si se puede reservar; por defecto true", example = "true") Boolean activa,
        @Schema(description = "Id de la sede", example = "1") @NotNull Long sedeId,
        @Schema(description = "Ids del equipamiento de la sala") Set<@NotNull Long> equipamientoIds) {
}
