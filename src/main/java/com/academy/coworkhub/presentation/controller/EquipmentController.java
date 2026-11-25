package com.academy.coworkhub.presentation.controller;

import com.academy.coworkhub.business.service.EquipmentService;
import com.academy.coworkhub.presentation.dto.request.EquipmentRequest;
import com.academy.coworkhub.presentation.dto.response.EquipmentResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Tag(name = "Equipamiento", description = "Equipamiento que se asigna a las salas (RF-01)")
@RequestMapping("/api/equipamientos")
public class EquipmentController {

    private final EquipmentService equipmentService;

    public EquipmentController(EquipmentService equipmentService) {
        this.equipmentService = equipmentService;
    }

    @Operation(summary = "Lista equipamientos", description = "Acceso: cualquier usuario autenticado.")
    @GetMapping
    public List<EquipmentResponse> findAll() {
        return equipmentService.findAll();
    }

    @Operation(summary = "Consulta un equipamiento", description = "Acceso: cualquier usuario autenticado.")
    @GetMapping("/{id}")
    public EquipmentResponse findById(@PathVariable Long id) {
        return equipmentService.findById(id);
    }

    @Operation(summary = "Crea un equipamiento", description = "Acceso: ADMIN. RN-11: el nombre es único.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EquipmentResponse create(@Valid @RequestBody EquipmentRequest request) {
        return equipmentService.create(request);
    }

    @Operation(summary = "Actualiza un equipamiento", description = "Acceso: ADMIN. RN-11: el nombre es único.")
    @PutMapping("/{id}")
    public EquipmentResponse update(@PathVariable Long id, @Valid @RequestBody EquipmentRequest request) {
        return equipmentService.update(id, request);
    }

    @Operation(summary = "Elimina un equipamiento", description = "Acceso: ADMIN. RN-12: no se elimina un equipamiento asignado a salas. Responde 200 sin cuerpo.")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        equipmentService.delete(id);
        return ResponseEntity.ok().build();
    }
}
