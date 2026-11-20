package com.academy.coworkhub.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EquipmentRequest(@NotBlank @Size(max = 100) String nombre) {
}
