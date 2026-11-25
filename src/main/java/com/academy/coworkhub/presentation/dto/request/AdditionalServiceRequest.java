package com.academy.coworkhub.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record AdditionalServiceRequest(
        @Schema(description = "Nombre único del servicio", example = "Catering") @NotBlank @Size(max = 100) String nombre,
        @Schema(description = "Precio por unidad", example = "25000.00") @NotNull @PositiveOrZero @Digits(integer = 8, fraction = 2) BigDecimal precioUnitario) {
}
