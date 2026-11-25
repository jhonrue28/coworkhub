package com.academy.coworkhub.presentation.controller;

import com.academy.coworkhub.business.service.BookingService;
import com.academy.coworkhub.presentation.dto.response.RoomResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@RestController
@Tag(name = "Disponibilidad", description = "Salas libres de una sede en un rango horario (RF-03)")
@RequestMapping("/api/disponibilidad")
public class AvailabilityController {

    private final BookingService bookingService;

    public AvailabilityController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @Operation(summary = "Salas libres", description = "Acceso: cualquier usuario autenticado. Devuelve las salas activas de la sede sin reservas PENDING o CONFIRMED que se crucen con el rango. Filtros opcionales: capacidadMinima y equipamientoIds (la sala debe tener todos). Fuera del horario de la sede la lista es vacía.")
    @GetMapping
    public List<RoomResponse> findAvailableRooms(
            @Parameter(description = "Id de la sede", example = "1") @RequestParam Long sedeId,
            @Parameter(description = "Fecha (yyyy-MM-dd)", example = "2030-06-03") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @Parameter(description = "Hora de inicio (HH:mm)", example = "10:00") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime horaInicio,
            @Parameter(description = "Hora de fin (HH:mm)", example = "12:00") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime horaFin,
            @Parameter(description = "Capacidad mínima de la sala", example = "4") @RequestParam(required = false) Integer capacidadMinima,
            @Parameter(description = "Ids de equipamiento que la sala debe tener (todos)", example = "1,2") @RequestParam(required = false) List<Long> equipamientoIds) {
        return bookingService.findAvailableRooms(sedeId, fecha, horaInicio, horaFin, capacidadMinima, equipamientoIds);
    }
}
