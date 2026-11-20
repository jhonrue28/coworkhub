package com.academy.coworkhub.presentation.dto.response;

import com.academy.coworkhub.persistence.entity.MembershipPlan;

public record MembershipPlanResponse(Long id, String nombre, int horasIncluidasMes,
                                     int descuentoExcedente, int maxReservasActivas) {

    public static MembershipPlanResponse from(MembershipPlan p) {
        return new MembershipPlanResponse(p.getId(), p.getName(), p.getIncludedHoursPerMonth(),
                p.getOverageDiscount(), p.getMaxActiveBookings());
    }
}
