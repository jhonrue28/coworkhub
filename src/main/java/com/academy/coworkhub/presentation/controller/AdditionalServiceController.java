package com.academy.coworkhub.presentation.controller;

import com.academy.coworkhub.business.service.AdditionalServiceService;
import com.academy.coworkhub.presentation.dto.request.AdditionalServiceRequest;
import com.academy.coworkhub.presentation.dto.response.AdditionalServiceResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/servicios-adicionales")
public class AdditionalServiceController {

    private final AdditionalServiceService additionalServiceService;

    public AdditionalServiceController(AdditionalServiceService additionalServiceService) {
        this.additionalServiceService = additionalServiceService;
    }

    @GetMapping
    public List<AdditionalServiceResponse> findAll() {
        return additionalServiceService.findAll();
    }

    @GetMapping("/{id}")
    public AdditionalServiceResponse findById(@PathVariable Long id) {
        return additionalServiceService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AdditionalServiceResponse create(@Valid @RequestBody AdditionalServiceRequest request) {
        return additionalServiceService.create(request);
    }

    @PutMapping("/{id}")
    public AdditionalServiceResponse update(@PathVariable Long id, @Valid @RequestBody AdditionalServiceRequest request) {
        return additionalServiceService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        additionalServiceService.delete(id);
        return ResponseEntity.ok().build();
    }
}
