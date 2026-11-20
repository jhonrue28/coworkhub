package com.academy.coworkhub.business.service;

import com.academy.coworkhub.presentation.dto.request.MembershipPlanRequest;
import com.academy.coworkhub.presentation.dto.response.MembershipPlanResponse;

import java.util.List;

public interface MembershipPlanService {

    List<MembershipPlanResponse> findAll();

    MembershipPlanResponse findById(Long id);

    MembershipPlanResponse create(MembershipPlanRequest request);

    MembershipPlanResponse update(Long id, MembershipPlanRequest request);

    void delete(Long id);
}
