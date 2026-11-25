package com.academy.coworkhub.presentation.controller;

import com.academy.coworkhub.business.service.BookingService;
import com.academy.coworkhub.business.service.MemberService;
import com.academy.coworkhub.persistence.entity.MemberStatus;
import com.academy.coworkhub.presentation.dto.request.MemberCreateRequest;
import com.academy.coworkhub.presentation.dto.request.MemberUpdateRequest;
import com.academy.coworkhub.presentation.dto.response.ConsumptionResponse;
import com.academy.coworkhub.presentation.dto.response.MemberResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Clock;
import java.time.YearMonth;
import java.util.List;

@RestController
@Tag(name = "Miembros", description = "Clientes del coworking, su cuenta de usuario y su consumo (RF-02, RF-10)")
@RequestMapping("/api/miembros")
@PreAuthorize(MemberController.STAFF)
public class MemberController {

    static final String STAFF = "hasAnyRole('ADMIN','RECEPTIONIST')";
    static final String STAFF_OR_SELF = STAFF + " or @ownership.isMember(#id)";

    private final MemberService memberService;
    private final BookingService bookingService;
    private final Clock clock;

    public MemberController(MemberService memberService, BookingService bookingService, Clock clock) {
        this.memberService = memberService;
        this.bookingService = bookingService;
        this.clock = clock;
    }

    @Operation(summary = "Lista los miembros", description = "Acceso: ADMIN y RECEPTIONIST. Filtros opcionales: nombre (contiene, sin distinguir mayúsculas) y estado.")
    @GetMapping
    public List<MemberResponse> search(@Parameter(description = "Parte del nombre completo", example = "gómez") @RequestParam(required = false) String nombre,
                                       @Parameter(description = "Estado del miembro") @RequestParam(required = false) MemberStatus estado) {
        return memberService.search(nombre, estado);
    }

    @Operation(summary = "Consulta un miembro", description = "Acceso: ADMIN y RECEPTIONIST. Un MEMBER solo puede consultar el suyo.")
    @PreAuthorize(STAFF_OR_SELF)
    @GetMapping("/{id}")
    public MemberResponse findById(@PathVariable Long id) {
        return memberService.findById(id);
    }

    @Operation(summary = "Registra un miembro y su usuario", description = "Acceso: ADMIN y RECEPTIONIST. Crea el miembro (ACTIVE) y su cuenta con rol MEMBER en una sola operación. RN-11: documento, email y usuario son únicos.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MemberResponse create(@Valid @RequestBody MemberCreateRequest request) {
        return memberService.create(request);
    }

    @Operation(summary = "Actualiza nombre, email y plan", description = "Acceso: ADMIN y RECEPTIONIST. RN-11: el email es único.")
    @PutMapping("/{id}")
    public MemberResponse update(@PathVariable Long id, @Valid @RequestBody MemberUpdateRequest request) {
        return memberService.update(id, request);
    }

    @Operation(summary = "Suspende un miembro", description = "Acceso: ADMIN y RECEPTIONIST. Un miembro suspendido no puede reservar (RN-05).")
    @PatchMapping("/{id}/suspender")
    public MemberResponse suspend(@PathVariable Long id) {
        return memberService.changeStatus(id, MemberStatus.SUSPENDED);
    }

    @Operation(summary = "Reactiva un miembro", description = "Acceso: ADMIN y RECEPTIONIST.")
    @PatchMapping("/{id}/activar")
    public MemberResponse activate(@PathVariable Long id) {
        return memberService.changeStatus(id, MemberStatus.ACTIVE);
    }

    @Operation(summary = "Elimina un miembro y su usuario", description = "Acceso: ADMIN y RECEPTIONIST. RN-12: no se elimina un miembro con reservas. Responde 200 sin cuerpo.")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        memberService.delete(id);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Consumo mensual de horas", description = "Acceso: ADMIN y RECEPTIONIST. Un MEMBER solo puede consultar el suyo. Horas incluidas en el plan, usadas (reservas PENDING, CONFIRMED y COMPLETED del mes), restantes y excedentes. Sin ?mes= usa el mes actual.")
    @PreAuthorize(STAFF_OR_SELF)
    @GetMapping("/{id}/consumo")
    public ConsumptionResponse consumption(@PathVariable Long id,
                                           @Parameter(description = "Mes (yyyy-MM); por defecto, el actual", example = "2030-06", schema = @Schema(type = "string")) @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth mes) {
        return bookingService.getConsumption(id, mes != null ? mes : YearMonth.now(clock));
    }
}
