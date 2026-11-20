package com.academy.coworkhub.presentation.dto.response;

import com.academy.coworkhub.persistence.entity.Location;

import java.time.LocalTime;

public record LocationResponse(Long id, String nombre, String ciudad, String direccion,
                               LocalTime horaApertura, LocalTime horaCierre) {

    public static LocationResponse from(Location l) {
        return new LocationResponse(l.getId(), l.getName(), l.getCity(), l.getAddress(),
                l.getOpeningTime(), l.getClosingTime());
    }
}
