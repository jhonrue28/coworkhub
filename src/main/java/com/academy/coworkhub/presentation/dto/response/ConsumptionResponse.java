package com.academy.coworkhub.presentation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Consumo mensual de horas de un miembro (RF-10)")
public record ConsumptionResponse(
        @Schema(description = "Id del miembro", example = "1") Long miembroId,
        @Schema(description = "Mes consultado (yyyy-MM)", example = "2030-06") String mes,
        @Schema(description = "Horas que incluye el plan", example = "10.00") BigDecimal horasIncluidas,
        @Schema(description = "Horas de reservas PENDING, CONFIRMED y COMPLETED del mes", example = "12.50") BigDecimal horasUsadas,
        @Schema(description = "Horas incluidas que quedan", example = "0.00") BigDecimal horasRestantes,
        @Schema(description = "Horas usadas por encima de las incluidas", example = "2.50") BigDecimal horasExcedentes) {
}
