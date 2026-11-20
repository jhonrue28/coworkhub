package com.academy.coworkhub.presentation.controller;

import com.academy.coworkhub.business.service.MembershipPlanService;
import com.academy.coworkhub.presentation.dto.request.MembershipPlanRequest;
import com.academy.coworkhub.presentation.dto.response.MembershipPlanResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/planes")
public class MembershipPlanController {

    private final MembershipPlanService planService;

    public MembershipPlanController(MembershipPlanService planService) {
        this.planService = planService;
    }

    @GetMapping
    public List<MembershipPlanResponse> findAll() {
        return planService.findAll();
    }

    @GetMapping("/{id}")
    public MembershipPlanResponse findById(@PathVariable Long id) {
        return planService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MembershipPlanResponse create(@Valid @RequestBody MembershipPlanRequest request) {
        return planService.create(request);
    }

    @PutMapping("/{id}")
    public MembershipPlanResponse update(@PathVariable Long id, @Valid @RequestBody MembershipPlanRequest request) {
        return planService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        planService.delete(id);
        return ResponseEntity.ok().build();
    }
}
