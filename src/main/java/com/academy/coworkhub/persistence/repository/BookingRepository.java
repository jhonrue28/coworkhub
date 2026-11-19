package com.academy.coworkhub.persistence.repository;

import com.academy.coworkhub.persistence.entity.Booking;
import com.academy.coworkhub.persistence.entity.BookingStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    // Listados con sala, miembro y detalles en una sola consulta (sin N+1)
    @EntityGraph(attributePaths = {"room", "member", "items", "items.service"})
    List<Booking> findByMemberIdOrderByStartTime(Long memberId);

    @EntityGraph(attributePaths = {"room", "member", "items", "items.service"})
    List<Booking> findByRoomIdAndStartTimeBetweenOrderByStartTime(Long roomId, LocalDateTime from, LocalDateTime to);

    @EntityGraph(attributePaths = {"room", "member", "items", "items.service"})
    List<Booking> findByStatusOrderByStartTime(BookingStatus status);

    // RN-01: se cruzan si empieza antes de que termine la otra y termina después de que empiece
    boolean existsByRoomIdAndStatusInAndStartTimeLessThanAndEndTimeGreaterThan(
            Long roomId, Collection<BookingStatus> statuses, LocalDateTime end, LocalDateTime start);

    // RN-06: reservas activas futuras del miembro
    long countByMemberIdAndStatusInAndStartTimeAfter(Long memberId, Collection<BookingStatus> statuses, LocalDateTime now);

    // RF-10 / RN-07: reservas del miembro que consumen horas en un mes
    List<Booking> findByMemberIdAndStatusInAndStartTimeGreaterThanEqualAndStartTimeLessThan(
            Long memberId, Collection<BookingStatus> statuses, LocalDateTime from, LocalDateTime to);

    boolean existsByRoomId(Long roomId);

    boolean existsByMemberId(Long memberId);
}
