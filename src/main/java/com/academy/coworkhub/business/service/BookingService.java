package com.academy.coworkhub.business.service;

import com.academy.coworkhub.persistence.entity.BookingStatus;
import com.academy.coworkhub.presentation.dto.request.BookingItemRequest;
import com.academy.coworkhub.presentation.dto.request.BookingRequest;
import com.academy.coworkhub.presentation.dto.response.BookingResponse;
import com.academy.coworkhub.presentation.dto.response.ConsumptionResponse;
import com.academy.coworkhub.presentation.dto.response.RoomResponse;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;

public interface BookingService {

    BookingResponse create(BookingRequest request);

    BookingResponse findById(Long id);

    /** Exactamente un filtro: memberId, roomId + date, o status. */
    List<BookingResponse> search(Long memberId, Long roomId, LocalDate date, BookingStatus status);

    BookingResponse addService(Long bookingId, BookingItemRequest request);

    BookingResponse removeService(Long bookingId, Long serviceId);

    BookingResponse confirm(Long id);

    BookingResponse cancel(Long id);

    BookingResponse complete(Long id);

    List<RoomResponse> findAvailableRooms(Long locationId, LocalDate date, LocalTime startTime, LocalTime endTime,
                                          Integer minCapacity, List<Long> equipmentIds);

    ConsumptionResponse getConsumption(Long memberId, YearMonth month);
}
