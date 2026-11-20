package com.academy.coworkhub.business.service;

import com.academy.coworkhub.presentation.dto.request.RoomRequest;
import com.academy.coworkhub.presentation.dto.response.RoomResponse;

import java.util.List;

public interface RoomService {

    /** Todas las salas, o solo las de una sede si locationId no es null. */
    List<RoomResponse> findAll(Long locationId);

    RoomResponse findById(Long id);

    RoomResponse create(RoomRequest request);

    RoomResponse update(Long id, RoomRequest request);

    void delete(Long id);
}
