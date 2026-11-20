package com.academy.coworkhub.business.service;

import com.academy.coworkhub.presentation.dto.request.LocationRequest;
import com.academy.coworkhub.presentation.dto.response.LocationResponse;

import java.util.List;

public interface LocationService {

    List<LocationResponse> findAll();

    LocationResponse findById(Long id);

    LocationResponse create(LocationRequest request);

    LocationResponse update(Long id, LocationRequest request);

    void delete(Long id);
}
