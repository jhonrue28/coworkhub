package com.academy.coworkhub.presentation.controller;

import com.academy.coworkhub.business.service.BookingService;
import com.academy.coworkhub.persistence.entity.BookingStatus;
import com.academy.coworkhub.presentation.dto.request.BookingItemRequest;
import com.academy.coworkhub.presentation.dto.request.BookingRequest;
import com.academy.coworkhub.presentation.dto.response.BookingResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@Tag(name = "Reservas", description = "Reservas de salas con sus servicios adicionales (RF-04 a RF-09)")
@RequestMapping("/api/reservas")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @Operation(summary = "Crea una reserva", description = "Acceso: ADMIN, RECEPTIONIST y MEMBER (solo a su nombre). Queda PENDING y devuelve el costo calculado (RN-07). Valida RN-01 a RN-06.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BookingResponse create(@Valid @RequestBody BookingRequest request) {
        return bookingService.create(request);
    }

    @Operation(summary = "Consulta una reserva", description = "Acceso: ADMIN, RECEPTIONIST y MEMBER (solo si es suya).")
    @GetMapping("/{id}")
    public BookingResponse findById(@PathVariable Long id) {
        return bookingService.findById(id);
    }

    @Operation(summary = "Lista reservas con un filtro", description = "Acceso: ADMIN, RECEPTIONIST y MEMBER (solo con su propio miembroId). Use exactamente uno: ?miembroId=, ?salaId=&fecha= o ?estado=.")
    @GetMapping
    public List<BookingResponse> search(@Parameter(description = "Reservas de un miembro", example = "1") @RequestParam(required = false) Long miembroId,
                                        @Parameter(description = "Reservas de una sala (junto con fecha)", example = "1") @RequestParam(required = false) Long salaId,
                                        @Parameter(description = "Día de las reservas de la sala (yyyy-MM-dd)", example = "2030-06-03") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
                                        @Parameter(description = "Reservas en un estado") @RequestParam(required = false) BookingStatus estado) {
        return bookingService.search(miembroId, salaId, fecha, estado);
    }

    @Operation(summary = "Agrega un servicio", description = "Acceso: ADMIN, RECEPTIONIST y MEMBER (solo si es suya). Solo con la reserva PENDING (RN-10). Si el servicio ya está, suma la cantidad. Recalcula el costo.")
    @PostMapping("/{id}/servicios")
    public BookingResponse addService(@PathVariable Long id, @Valid @RequestBody BookingItemRequest request) {
        return bookingService.addService(id, request);
    }

    @Operation(summary = "Quita un servicio", description = "Acceso: ADMIN, RECEPTIONIST y MEMBER (solo si es suya). Solo con la reserva PENDING (RN-10). Recalcula el costo. Responde 200 sin cuerpo.")
    @DeleteMapping("/{id}/servicios/{servicioId}")
    public ResponseEntity<Void> removeService(@PathVariable Long id, @PathVariable Long servicioId) {
        bookingService.removeService(id, servicioId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Confirma una reserva", description = "Acceso: ADMIN y RECEPTIONIST. PENDING → CONFIRMED (RN-09).")
    @PostMapping("/{id}/confirmar")
    public BookingResponse confirm(@PathVariable Long id) {
        return bookingService.confirm(id);
    }

    @Operation(summary = "Cancela una reserva", description = "Acceso: ADMIN, RECEPTIONIST y MEMBER (solo si es suya). RN-08: con 24 h o más, sin cargo; entre 24 h y 2 h, 50 % del costo de la sala; con menos de 2 h o ya empezada, 409. Devuelve el cargo en cargoCancelacion.")
    @PostMapping("/{id}/cancelar")
    public BookingResponse cancel(@PathVariable Long id) {
        return bookingService.cancel(id);
    }

    @Operation(summary = "Completa una reserva", description = "Acceso: ADMIN y RECEPTIONIST. CONFIRMED → COMPLETED (RN-09).")
    @PostMapping("/{id}/completar")
    public BookingResponse complete(@PathVariable Long id) {
        return bookingService.complete(id);
    }
}
