package com.academy.coworkhub.presentation.dto.response;

import com.academy.coworkhub.persistence.entity.AdditionalService;

import java.math.BigDecimal;

public record AdditionalServiceResponse(Long id, String nombre, BigDecimal precioUnitario) {

    public static AdditionalServiceResponse from(AdditionalService s) {
        return new AdditionalServiceResponse(s.getId(), s.getName(), s.getUnitPrice());
    }
}
