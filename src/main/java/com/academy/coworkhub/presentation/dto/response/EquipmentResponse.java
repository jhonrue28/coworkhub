package com.academy.coworkhub.presentation.dto.response;

import com.academy.coworkhub.persistence.entity.Equipment;

public record EquipmentResponse(Long id, String nombre) {

    public static EquipmentResponse from(Equipment e) {
        return new EquipmentResponse(e.getId(), e.getName());
    }
}
