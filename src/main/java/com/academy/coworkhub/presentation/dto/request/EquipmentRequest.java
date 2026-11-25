package com.academy.coworkhub.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EquipmentRequest(
        @Schema(description = "Nombre único del equipamiento", example = "Proyector") @NotBlank @Size(max = 100) String nombre) {
}
