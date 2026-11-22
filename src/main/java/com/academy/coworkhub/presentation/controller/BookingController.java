package com.academy.coworkhub.presentation.controller;

import com.academy.coworkhub.business.service.BookingService;
import com.academy.coworkhub.persistence.entity.BookingStatus;
import com.academy.coworkhub.presentation.dto.request.BookingItemRequest;
import com.academy.coworkhub.presentation.dto.request.BookingRequest;
import com.academy.coworkhub.presentation.dto.response.BookingResponse;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/reservas")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BookingResponse create(@Valid @RequestBody BookingRequest request) {
        return bookingService.create(request);
    }

    @GetMapping("/{id}")
    public BookingResponse findById(@PathVariable Long id) {
        return bookingService.findById(id);
    }

    @GetMapping
    public List<BookingResponse> search(@RequestParam(required = false) Long miembroId,
                                        @RequestParam(required = false) Long salaId,
                                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
                                        @RequestParam(required = false) BookingStatus estado) {
        return bookingService.search(miembroId, salaId, fecha, estado);
    }

    @PostMapping("/{id}/servicios")
    public BookingResponse addService(@PathVariable Long id, @Valid @RequestBody BookingItemRequest request) {
        return bookingService.addService(id, request);
    }

    @DeleteMapping("/{id}/servicios/{servicioId}")
    public ResponseEntity<Void> removeService(@PathVariable Long id, @PathVariable Long servicioId) {
        bookingService.removeService(id, servicioId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/confirmar")
    public BookingResponse confirm(@PathVariable Long id) {
        return bookingService.confirm(id);
    }

    @PostMapping("/{id}/cancelar")
    public BookingResponse cancel(@PathVariable Long id) {
        return bookingService.cancel(id);
    }

    @PostMapping("/{id}/completar")
    public BookingResponse complete(@PathVariable Long id) {
        return bookingService.complete(id);
    }
}
