package com.academy.coworkhub.presentation.dto.request;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record AdditionalServiceRequest(
        @NotBlank @Size(max = 100) String nombre,
        @NotNull @PositiveOrZero @Digits(integer = 8, fraction = 2) BigDecimal precioUnitario) {
}
