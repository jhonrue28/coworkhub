package com.academy.coworkhub.presentation.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalTime;

public record LocationRequest(
        @NotBlank @Size(max = 100) String nombre,
        @NotBlank @Size(max = 100) String ciudad,
        @NotBlank @Size(max = 200) String direccion,
        @NotNull LocalTime horaApertura,
        @NotNull LocalTime horaCierre) {

    @JsonIgnore
    @AssertTrue(message = "horaApertura debe ser anterior a horaCierre")
    public boolean isHorarioValido() {
        return horaApertura == null || horaCierre == null || horaApertura.isBefore(horaCierre);
    }
}
