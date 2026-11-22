package com.academy.coworkhub.persistence.entity;

public enum BookingStatus {
    PENDING, CONFIRMED, COMPLETED, CANCELLED;

    /** RN-09: PENDING → CONFIRMED → COMPLETED, o PENDING/CONFIRMED → CANCELLED. Ninguna otra. */
    public boolean canTransitionTo(BookingStatus next) {
        return switch (this) {
            case PENDING -> next == CONFIRMED || next == CANCELLED;
            case CONFIRMED -> next == COMPLETED || next == CANCELLED;
            case COMPLETED, CANCELLED -> false;
        };
    }
}
