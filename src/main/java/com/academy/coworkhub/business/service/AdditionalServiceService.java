package com.academy.coworkhub.business.service;

import com.academy.coworkhub.presentation.dto.request.AdditionalServiceRequest;
import com.academy.coworkhub.presentation.dto.response.AdditionalServiceResponse;

import java.util.List;

public interface AdditionalServiceService {

    List<AdditionalServiceResponse> findAll();

    AdditionalServiceResponse findById(Long id);

    AdditionalServiceResponse create(AdditionalServiceRequest request);

    AdditionalServiceResponse update(Long id, AdditionalServiceRequest request);

    void delete(Long id);
}
