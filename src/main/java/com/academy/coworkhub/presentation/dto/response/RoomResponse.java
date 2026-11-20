package com.academy.coworkhub.presentation.dto.response;

import com.academy.coworkhub.persistence.entity.Room;
import com.academy.coworkhub.persistence.entity.RoomType;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

public record RoomResponse(Long id, String nombre, RoomType tipo, int capacidad, BigDecimal tarifaHora,
                           boolean activa, Long sedeId, String sedeNombre, List<EquipmentResponse> equipamiento) {

    public static RoomResponse from(Room r) {
        return new RoomResponse(r.getId(), r.getName(), r.getType(), r.getCapacity(), r.getHourlyRate(),
                r.isActive(), r.getLocation().getId(), r.getLocation().getName(),
                r.getEquipment().stream()
                        .sorted(Comparator.comparing(e -> e.getId()))
                        .map(EquipmentResponse::from)
                        .toList());
    }
}
