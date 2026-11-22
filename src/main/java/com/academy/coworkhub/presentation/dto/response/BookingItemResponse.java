package com.academy.coworkhub.presentation.dto.response;

import com.academy.coworkhub.persistence.entity.BookingItem;

import java.math.BigDecimal;

public record BookingItemResponse(Long servicioId, String servicioNombre, int cantidad,
                                  BigDecimal precioUnitarioAplicado, BigDecimal subtotal) {

    public static BookingItemResponse from(BookingItem i) {
        return new BookingItemResponse(i.getService().getId(), i.getService().getName(), i.getQuantity(),
                i.getAppliedUnitPrice(), i.getAppliedUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity())));
    }
}
