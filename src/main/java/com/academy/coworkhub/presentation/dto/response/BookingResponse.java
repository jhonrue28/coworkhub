package com.academy.coworkhub.presentation.dto.response;

import com.academy.coworkhub.persistence.entity.Booking;
import com.academy.coworkhub.persistence.entity.BookingStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record BookingResponse(Long id, Long salaId, String salaNombre, Long miembroId, String miembroNombre,
                              LocalDateTime inicio, LocalDateTime fin, int asistentes, BookingStatus estado,
                              BigDecimal costoSala, BigDecimal costoServicios, BigDecimal costoTotal,
                              BigDecimal cargoCancelacion, List<BookingItemResponse> servicios) {

    public static BookingResponse from(Booking b) {
        return new BookingResponse(b.getId(), b.getRoom().getId(), b.getRoom().getName(),
                b.getMember().getId(), b.getMember().getFullName(), b.getStartTime(), b.getEndTime(),
                b.getAttendees(), b.getStatus(), b.getRoomCost(), b.getServicesCost(), b.getTotalCost(),
                b.getCancellationFee(), b.getItems().stream().map(BookingItemResponse::from).toList());
    }
}
