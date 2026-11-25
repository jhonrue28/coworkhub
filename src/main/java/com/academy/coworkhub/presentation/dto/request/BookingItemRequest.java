package com.academy.coworkhub.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record BookingItemRequest(
        @Schema(description = "Id del servicio adicional", example = "1") @NotNull Long servicioId,
        @Schema(description = "Cantidad", example = "2") @NotNull @Positive Integer cantidad) {
}
