package com.academy.coworkhub.business.service;

import com.academy.coworkhub.presentation.dto.request.EquipmentRequest;
import com.academy.coworkhub.presentation.dto.response.EquipmentResponse;

import java.util.List;

public interface EquipmentService {

    List<EquipmentResponse> findAll();

    EquipmentResponse findById(Long id);

    EquipmentResponse create(EquipmentRequest request);

    EquipmentResponse update(Long id, EquipmentRequest request);

    void delete(Long id);
}
