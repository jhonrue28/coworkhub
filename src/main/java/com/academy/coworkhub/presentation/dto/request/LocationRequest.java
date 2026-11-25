package com.academy.coworkhub.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalTime;

public record LocationRequest(
        @Schema(description = "Nombre de la sede", example = "Sede Centro") @NotBlank @Size(max = 100) String nombre,
        @Schema(description = "Ciudad", example = "Bogotá") @NotBlank @Size(max = 100) String ciudad,
        @Schema(description = "Dirección", example = "Carrera 7 # 12-34") @NotBlank @Size(max = 200) String direccion,
        @Schema(description = "Hora de apertura (HH:mm)", example = "07:00") @NotNull LocalTime horaApertura,
        @Schema(description = "Hora de cierre (HH:mm), posterior a la apertura", example = "21:00") @NotNull LocalTime horaCierre) {

    @JsonIgnore
    @AssertTrue(message = "horaApertura debe ser anterior a horaCierre")
    public boolean isHorarioValido() {
        return horaApertura == null || horaCierre == null || horaApertura.isBefore(horaCierre);
    }
}
