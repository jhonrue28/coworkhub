package com.academy.coworkhub.presentation.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record BookingItemRequest(@NotNull Long servicioId, @NotNull @Positive Integer cantidad) {
}
