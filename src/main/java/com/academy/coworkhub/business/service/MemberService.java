package com.academy.coworkhub.business.service;

import com.academy.coworkhub.persistence.entity.MemberStatus;
import com.academy.coworkhub.presentation.dto.request.MemberCreateRequest;
import com.academy.coworkhub.presentation.dto.request.MemberUpdateRequest;
import com.academy.coworkhub.presentation.dto.response.MemberResponse;

import java.util.List;

public interface MemberService {

    /** Filtros opcionales: null no filtra. */
    List<MemberResponse> search(String name, MemberStatus status);

    MemberResponse findById(Long id);

    MemberResponse create(MemberCreateRequest request);

    MemberResponse update(Long id, MemberUpdateRequest request);

    MemberResponse changeStatus(Long id, MemberStatus status);

    void delete(Long id);
}
