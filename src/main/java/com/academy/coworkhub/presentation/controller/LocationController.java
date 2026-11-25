package com.academy.coworkhub.presentation.controller;

import com.academy.coworkhub.business.service.LocationService;
import com.academy.coworkhub.presentation.dto.request.LocationRequest;
import com.academy.coworkhub.presentation.dto.response.LocationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Tag(name = "Sedes", description = "Sedes del coworking y su horario de atención (RF-01)")
@RequestMapping("/api/sedes")
public class LocationController {

    private final LocationService locationService;

    public LocationController(LocationService locationService) {
        this.locationService = locationService;
    }

    @Operation(summary = "Lista sedes", description = "Acceso: cualquier usuario autenticado.")
    @GetMapping
    public List<LocationResponse> findAll() {
        return locationService.findAll();
    }

    @Operation(summary = "Consulta una sede", description = "Acceso: cualquier usuario autenticado.")
    @GetMapping("/{id}")
    public LocationResponse findById(@PathVariable Long id) {
        return locationService.findById(id);
    }

    @Operation(summary = "Crea una sede", description = "Acceso: ADMIN. RN-11: el nombre es único.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LocationResponse create(@Valid @RequestBody LocationRequest request) {
        return locationService.create(request);
    }

    @Operation(summary = "Actualiza una sede", description = "Acceso: ADMIN. RN-11: el nombre es único.")
    @PutMapping("/{id}")
    public LocationResponse update(@PathVariable Long id, @Valid @RequestBody LocationRequest request) {
        return locationService.update(id, request);
    }

    @Operation(summary = "Elimina una sede", description = "Acceso: ADMIN. RN-12: no se elimina una sede con salas. Responde 200 sin cuerpo.")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        locationService.delete(id);
        return ResponseEntity.ok().build();
    }
}
