package com.academy.coworkhub.presentation.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDateTime;
import java.util.List;

public record BookingRequest(
        @NotNull Long salaId,
        @NotNull Long miembroId,
        @NotNull LocalDateTime inicio,
        @NotNull LocalDateTime fin,
        @NotNull @Positive Integer asistentes,
        List<@Valid @NotNull BookingItemRequest> servicios) {
}
