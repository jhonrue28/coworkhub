package com.academy.coworkhub.presentation.controller;

import com.academy.coworkhub.business.service.MembershipPlanService;
import com.academy.coworkhub.presentation.dto.request.MembershipPlanRequest;
import com.academy.coworkhub.presentation.dto.response.MembershipPlanResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Tag(name = "Planes", description = "Planes de membresía: horas incluidas, descuento y máximo de reservas (RF-01)")
@RequestMapping("/api/planes")
public class MembershipPlanController {

    private final MembershipPlanService planService;

    public MembershipPlanController(MembershipPlanService planService) {
        this.planService = planService;
    }

    @Operation(summary = "Lista planes", description = "Acceso: cualquier usuario autenticado.")
    @GetMapping
    public List<MembershipPlanResponse> findAll() {
        return planService.findAll();
    }

    @Operation(summary = "Consulta un plan", description = "Acceso: cualquier usuario autenticado.")
    @GetMapping("/{id}")
    public MembershipPlanResponse findById(@PathVariable Long id) {
        return planService.findById(id);
    }

    @Operation(summary = "Crea un plan", description = "Acceso: ADMIN. RN-11: el nombre es único.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MembershipPlanResponse create(@Valid @RequestBody MembershipPlanRequest request) {
        return planService.create(request);
    }

    @Operation(summary = "Actualiza un plan", description = "Acceso: ADMIN. RN-11: el nombre es único.")
    @PutMapping("/{id}")
    public MembershipPlanResponse update(@PathVariable Long id, @Valid @RequestBody MembershipPlanRequest request) {
        return planService.update(id, request);
    }

    @Operation(summary = "Elimina un plan", description = "Acceso: ADMIN. RN-12: no se elimina un plan con miembros. Responde 200 sin cuerpo.")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        planService.delete(id);
        return ResponseEntity.ok().build();
    }
}
