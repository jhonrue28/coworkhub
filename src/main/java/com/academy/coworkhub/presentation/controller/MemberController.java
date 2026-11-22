package com.academy.coworkhub.presentation.controller;

import com.academy.coworkhub.business.service.BookingService;
import com.academy.coworkhub.business.service.MemberService;
import com.academy.coworkhub.persistence.entity.MemberStatus;
import com.academy.coworkhub.presentation.dto.request.MemberCreateRequest;
import com.academy.coworkhub.presentation.dto.request.MemberUpdateRequest;
import com.academy.coworkhub.presentation.dto.response.ConsumptionResponse;
import com.academy.coworkhub.presentation.dto.response.MemberResponse;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Clock;
import java.time.YearMonth;
import java.util.List;

@RestController
@RequestMapping("/api/miembros")
public class MemberController {

    private final MemberService memberService;
    private final BookingService bookingService;
    private final Clock clock;

    public MemberController(MemberService memberService, BookingService bookingService, Clock clock) {
        this.memberService = memberService;
        this.bookingService = bookingService;
        this.clock = clock;
    }

    @GetMapping
    public List<MemberResponse> search(@RequestParam(required = false) String nombre,
                                       @RequestParam(required = false) MemberStatus estado) {
        return memberService.search(nombre, estado);
    }

    @GetMapping("/{id}")
    public MemberResponse findById(@PathVariable Long id) {
        return memberService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MemberResponse create(@Valid @RequestBody MemberCreateRequest request) {
        return memberService.create(request);
    }

    @PutMapping("/{id}")
    public MemberResponse update(@PathVariable Long id, @Valid @RequestBody MemberUpdateRequest request) {
        return memberService.update(id, request);
    }

    @PatchMapping("/{id}/suspender")
    public MemberResponse suspend(@PathVariable Long id) {
        return memberService.changeStatus(id, MemberStatus.SUSPENDED);
    }

    @PatchMapping("/{id}/activar")
    public MemberResponse activate(@PathVariable Long id) {
        return memberService.changeStatus(id, MemberStatus.ACTIVE);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        memberService.delete(id);
        return ResponseEntity.ok().build();
    }

    /** RF-10. Sin ?mes= devuelve el mes actual. */
    @GetMapping("/{id}/consumo")
    public ConsumptionResponse consumption(@PathVariable Long id,
                                           @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth mes) {
        return bookingService.getConsumption(id, mes != null ? mes : YearMonth.now(clock));
    }
}
