package com.academy.coworkhub.presentation.dto.response;

import java.math.BigDecimal;

/** RF-10: consumo de horas del plan en un mes (formato yyyy-MM). */
public record ConsumptionResponse(Long miembroId, String mes, BigDecimal horasIncluidas, BigDecimal horasUsadas,
                                  BigDecimal horasRestantes, BigDecimal horasExcedentes) {
}
