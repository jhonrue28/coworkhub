package com.academy.coworkhub.presentation.controller;

import com.academy.coworkhub.business.service.AdditionalServiceService;
import com.academy.coworkhub.presentation.dto.request.AdditionalServiceRequest;
import com.academy.coworkhub.presentation.dto.response.AdditionalServiceResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Tag(name = "Servicios adicionales", description = "Servicios que se agregan a una reserva (RF-01)")
@RequestMapping("/api/servicios-adicionales")
public class AdditionalServiceController {

    private final AdditionalServiceService additionalServiceService;

    public AdditionalServiceController(AdditionalServiceService additionalServiceService) {
        this.additionalServiceService = additionalServiceService;
    }

    @Operation(summary = "Lista servicios adicionales", description = "Acceso: cualquier usuario autenticado.")
    @GetMapping
    public List<AdditionalServiceResponse> findAll() {
        return additionalServiceService.findAll();
    }

    @Operation(summary = "Consulta un servicio adicional", description = "Acceso: cualquier usuario autenticado.")
    @GetMapping("/{id}")
    public AdditionalServiceResponse findById(@PathVariable Long id) {
        return additionalServiceService.findById(id);
    }

    @Operation(summary = "Crea un servicio adicional", description = "Acceso: ADMIN. RN-11: el nombre es único.")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AdditionalServiceResponse create(@Valid @RequestBody AdditionalServiceRequest request) {
        return additionalServiceService.create(request);
    }

    @Operation(summary = "Actualiza un servicio adicional", description = "Acceso: ADMIN. RN-11: el nombre es único.")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public AdditionalServiceResponse update(@PathVariable Long id, @Valid @RequestBody AdditionalServiceRequest request) {
        return additionalServiceService.update(id, request);
    }

    @Operation(summary = "Elimina un servicio adicional", description = "Acceso: ADMIN. RN-12: no se elimina un servicio usado en reservas. Responde 200 sin cuerpo.")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        additionalServiceService.delete(id);
        return ResponseEntity.ok().build();
    }
}
