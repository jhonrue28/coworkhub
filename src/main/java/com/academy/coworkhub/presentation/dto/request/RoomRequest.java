package com.academy.coworkhub.presentation.dto.request;

import com.academy.coworkhub.persistence.entity.RoomType;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.Set;

public record RoomRequest(
        @NotBlank @Size(max = 100) String nombre,
        @NotNull RoomType tipo,
        @NotNull @Positive Integer capacidad,
        @NotNull @Positive @Digits(integer = 8, fraction = 2) BigDecimal tarifaHora,
        Boolean activa,
        @NotNull Long sedeId,
        Set<@NotNull Long> equipamientoIds) {
}
