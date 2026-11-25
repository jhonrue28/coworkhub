package com.academy.coworkhub.business.security;

import com.academy.coworkhub.persistence.entity.UserAccount;
import com.academy.coworkhub.persistence.repository.BookingRepository;
import com.academy.coworkhub.persistence.repository.UserAccountRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * Dueño del recurso para @PreAuthorize: {@code @ownership.isMember(#id)} y {@code @ownership.isBookingOwner(#id)}.
 * Solo se evalúa para MEMBER; ADMIN y RECEPTIONIST pasan antes por hasAnyRole.
 */
@Service("ownership")
public class OwnershipService {

    private final UserAccountRepository userAccountRepository;
    private final BookingRepository bookingRepository;

    public OwnershipService(UserAccountRepository userAccountRepository, BookingRepository bookingRepository) {
        this.userAccountRepository = userAccountRepository;
        this.bookingRepository = bookingRepository;
    }

    /** El usuario autenticado es el miembro {@code memberId}. */
    public boolean isMember(Long memberId) {
        Long current = currentMemberId();
        return memberId != null && current != null && current.equals(memberId);
    }

    /** La reserva es del miembro autenticado. Si no existe, deja pasar para que el servicio responda 404. */
    public boolean isBookingOwner(Long bookingId) {
        Long current = currentMemberId();
        if (current == null) {
            return false;
        }
        return bookingRepository.findById(bookingId)
                .map(booking -> Objects.equals(booking.getMember().getId(), current))
                .orElse(true);
    }

    /** null si no hay sesión o si la cuenta es de un empleado. */
    private Long currentMemberId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return null;
        }
        return userAccountRepository.findByUsername(auth.getName())
                .map(UserAccount::getMember)
                .map(member -> member.getId())
                .orElse(null);
    }
}
