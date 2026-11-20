package com.academy.coworkhub.presentation.dto.response;

import com.academy.coworkhub.persistence.entity.Member;
import com.academy.coworkhub.persistence.entity.MemberStatus;

public record MemberResponse(Long id, String documento, String nombreCompleto, String email,
                             MemberStatus estado, Long planId, String planNombre) {

    public static MemberResponse from(Member m) {
        return new MemberResponse(m.getId(), m.getDocumentNumber(), m.getFullName(), m.getEmail(),
                m.getStatus(), m.getPlan().getId(), m.getPlan().getName());
    }
}
