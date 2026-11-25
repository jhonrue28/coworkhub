package com.academy.coworkhub.presentation.controller;

import com.academy.coworkhub.business.service.RoomService;
import com.academy.coworkhub.presentation.dto.request.RoomRequest;
import com.academy.coworkhub.presentation.dto.response.RoomResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Tag(name = "Salas", description = "Salas de cada sede con su equipamiento (RF-01)")
@RequestMapping("/api/salas")
public class RoomController {

    private final RoomService roomService;

    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    @Operation(summary = "Lista las salas", description = "Acceso: cualquier usuario autenticado. Con ?sedeId= devuelve solo las de esa sede.")
    @GetMapping
    public List<RoomResponse> findAll(@Parameter(description = "Solo las salas de esta sede", example = "1") @RequestParam(required = false) Long sedeId) {
        return roomService.findAll(sedeId);
    }

    @Operation(summary = "Consulta una sala", description = "Acceso: cualquier usuario autenticado.")
    @GetMapping("/{id}")
    public RoomResponse findById(@PathVariable Long id) {
        return roomService.findById(id);
    }

    @Operation(summary = "Crea una sala", description = "Acceso: ADMIN. RN-11: el nombre es único dentro de la sede.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RoomResponse create(@Valid @RequestBody RoomRequest request) {
        return roomService.create(request);
    }

    @Operation(summary = "Actualiza una sala", description = "Acceso: ADMIN. RN-11: el nombre es único dentro de la sede.")
    @PutMapping("/{id}")
    public RoomResponse update(@PathVariable Long id, @Valid @RequestBody RoomRequest request) {
        return roomService.update(id, request);
    }

    @Operation(summary = "Elimina una sala", description = "Acceso: ADMIN. RN-12: no se elimina una sala con reservas. Responde 200 sin cuerpo.")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        roomService.delete(id);
        return ResponseEntity.ok().build();
    }
}
