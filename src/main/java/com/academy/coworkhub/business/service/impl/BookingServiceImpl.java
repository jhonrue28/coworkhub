package com.academy.coworkhub.business.service.impl;

import com.academy.coworkhub.business.exception.BusinessRuleException;
import com.academy.coworkhub.business.exception.InvalidRequestException;
import com.academy.coworkhub.business.exception.ResourceNotFoundException;
import com.academy.coworkhub.business.service.BookingService;
import com.academy.coworkhub.persistence.entity.*;
import com.academy.coworkhub.persistence.repository.*;
import com.academy.coworkhub.presentation.dto.request.BookingItemRequest;
import com.academy.coworkhub.presentation.dto.request.BookingRequest;
import com.academy.coworkhub.presentation.dto.response.BookingResponse;
import com.academy.coworkhub.presentation.dto.response.ConsumptionResponse;
import com.academy.coworkhub.presentation.dto.response.RoomResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Transactional(readOnly = true)
public class BookingServiceImpl implements BookingService {

    /** Reservas que ocupan la sala (RN-01) y cuentan como activas (RN-06). */
    private static final Set<BookingStatus> ACTIVE = EnumSet.of(BookingStatus.PENDING, BookingStatus.CONFIRMED);
    /** Reservas que consumen horas del plan; las canceladas devuelven sus horas (RN-08). */
    private static final Set<BookingStatus> CONSUMING =
            EnumSet.of(BookingStatus.PENDING, BookingStatus.CONFIRMED, BookingStatus.COMPLETED);

    private static final Duration MIN_DURATION = Duration.ofMinutes(30);
    private static final Duration MAX_DURATION = Duration.ofHours(8);
    private static final Duration SLOT = Duration.ofMinutes(30);
    private static final Duration MIN_ADVANCE = Duration.ofHours(1);
    private static final Duration FREE_CANCELLATION = Duration.ofHours(24);
    private static final Duration MIN_CANCELLATION = Duration.ofHours(2);
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final BigDecimal MINUTES_PER_HOUR = BigDecimal.valueOf(60);
    private static final BigDecimal LATE_CANCELLATION_RATE = new BigDecimal("0.50");

    private final BookingRepository bookingRepository;
    private final RoomRepository roomRepository;
    private final MemberRepository memberRepository;
    private final LocationRepository locationRepository;
    private final AdditionalServiceRepository additionalServiceRepository;
    private final Clock clock;

    public BookingServiceImpl(BookingRepository bookingRepository, RoomRepository roomRepository,
                              MemberRepository memberRepository, LocationRepository locationRepository,
                              AdditionalServiceRepository additionalServiceRepository, Clock clock) {
        this.bookingRepository = bookingRepository;
        this.roomRepository = roomRepository;
        this.memberRepository = memberRepository;
        this.locationRepository = locationRepository;
        this.additionalServiceRepository = additionalServiceRepository;
        this.clock = clock;
    }

    // ---------------------------------------------------------------- RF-04

    @Override
    @Transactional
    public BookingResponse create(BookingRequest request) {
        LocalDateTime start = request.inicio();
        LocalDateTime end = request.fin();
        validateDuration(start, end);

        // Bloqueos en orden fijo (sala y después miembro) hasta el commit: RN-01 y RN-06 con solicitudes simultáneas
        Room room = roomRepository.findByIdForUpdate(request.salaId())
                .orElseThrow(() -> new ResourceNotFoundException("Sala", request.salaId()));
        Member member = memberRepository.findByIdForUpdate(request.miembroId())
                .orElseThrow(() -> new ResourceNotFoundException("Miembro", request.miembroId()));

        validateSchedule(room.getLocation(), start, end);
        if (!room.isActive()) {
            throw new BusinessRuleException("RN-04", "La sala no está activa");
        }
        if (request.asistentes() > room.getCapacity()) {
            throw new BusinessRuleException("RN-04",
                    "Los asistentes (" + request.asistentes() + ") superan la capacidad de la sala (" + room.getCapacity() + ")");
        }
        if (member.getStatus() != MemberStatus.ACTIVE) {
            throw new BusinessRuleException("RN-05", "El miembro está suspendido y no puede reservar");
        }
        long activeBookings = bookingRepository.countByMemberIdAndStatusInAndStartTimeAfter(member.getId(), ACTIVE, now());
        if (activeBookings >= member.getPlan().getMaxActiveBookings()) {
            throw new BusinessRuleException("RN-06",
                    "El miembro alcanzó el máximo de " + member.getPlan().getMaxActiveBookings() + " reservas activas de su plan");
        }
        if (bookingRepository.existsByRoomIdAndStatusInAndStartTimeLessThanAndEndTimeGreaterThan(room.getId(), ACTIVE, end, start)) {
            throw new BusinessRuleException("RN-01", "La sala ya está reservada en ese horario");
        }

        Booking booking = new Booking();
        booking.setRoom(room);
        booking.setMember(member);
        booking.setStartTime(start);
        booking.setEndTime(end);
        booking.setAttendees(request.asistentes());
        booking.setStatus(BookingStatus.PENDING);
        if (request.servicios() != null) {
            request.servicios().forEach(item -> addOrIncrease(booking, item));
        }
        booking.setRoomCost(roomCost(member, room.getHourlyRate(), start, end));
        recalculateTotals(booking);
        return BookingResponse.from(bookingRepository.save(booking));
    }

    /** RN-02: entre 30 min y 8 h, en múltiplos de 30 min, el mismo día. */
    private void validateDuration(LocalDateTime start, LocalDateTime end) {
        Duration duration = Duration.between(start, end);
        if (!start.toLocalDate().equals(end.toLocalDate())) {
            throw new BusinessRuleException("RN-02", "La reserva debe empezar y terminar el mismo día");
        }
        if (duration.compareTo(MIN_DURATION) < 0 || duration.compareTo(MAX_DURATION) > 0
                || duration.toSeconds() % SLOT.toSeconds() != 0) {
            throw new BusinessRuleException("RN-02", "La reserva debe durar entre 30 min y 8 h, en múltiplos de 30 min");
        }
    }

    /** RN-03: dentro del horario de la sede y con al menos 1 h de anticipación. */
    private void validateSchedule(Location location, LocalDateTime start, LocalDateTime end) {
        if (start.toLocalTime().isBefore(location.getOpeningTime()) || end.toLocalTime().isAfter(location.getClosingTime())) {
            throw new BusinessRuleException("RN-03", "La reserva debe estar dentro del horario de la sede ("
                    + location.getOpeningTime() + " - " + location.getClosingTime() + ")");
        }
        if (start.isBefore(now().plus(MIN_ADVANCE))) {
            throw new BusinessRuleException("RN-03", "La reserva debe ser futura y hacerse con al menos 1 h de anticipación");
        }
    }

    // ---------------------------------------------------------------- RN-07

    /** Las horas que el plan incluye en el mes no se cobran; las excedentes valen hourlyRate × (1 − descuento). */
    private BigDecimal roomCost(Member member, BigDecimal hourlyRate, LocalDateTime start, LocalDateTime end) {
        MembershipPlan plan = member.getPlan();
        BigDecimal remaining = BigDecimal.valueOf(plan.getIncludedHoursPerMonth())
                .subtract(usedHours(member.getId(), YearMonth.from(start)))
                .max(BigDecimal.ZERO);
        BigDecimal overage = hours(start, end).subtract(remaining).max(BigDecimal.ZERO);
        return overage.multiply(hourlyRate)
                .multiply(HUNDRED.subtract(BigDecimal.valueOf(plan.getOverageDiscount())))
                .divide(HUNDRED, 2, RoundingMode.HALF_UP);
    }

    private BigDecimal usedHours(Long memberId, YearMonth month) {
        return bookingRepository.findByMemberIdAndStatusInAndStartTimeGreaterThanEqualAndStartTimeLessThan(
                        memberId, CONSUMING, month.atDay(1).atStartOfDay(), month.plusMonths(1).atDay(1).atStartOfDay())
                .stream()
                .map(b -> hours(b.getStartTime(), b.getEndTime()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static BigDecimal hours(LocalDateTime start, LocalDateTime end) {
        return BigDecimal.valueOf(Duration.between(start, end).toMinutes()).divide(MINUTES_PER_HOUR, 2, RoundingMode.HALF_UP);
    }

    /** Servicios: quantity × appliedUnitPrice; total = sala + servicios. */
    private static void recalculateTotals(Booking booking) {
        BigDecimal services = booking.getItems().stream()
                .map(i -> i.getAppliedUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        booking.setServicesCost(services);
        booking.setTotalCost(booking.getRoomCost().add(services));
    }

    /** Un detalle por servicio: si ya está, suma la cantidad y conserva el precio con que se agregó. */
    private void addOrIncrease(Booking booking, BookingItemRequest request) {
        AdditionalService service = additionalServiceRepository.findById(request.servicioId())
                .orElseThrow(() -> new ResourceNotFoundException("Servicio adicional", request.servicioId()));
        booking.getItems().stream()
                .filter(i -> i.getService().getId().equals(service.getId()))
                .findFirst()
                .ifPresentOrElse(
                        i -> i.setQuantity(i.getQuantity() + request.cantidad()),
                        () -> {
                            BookingItem item = new BookingItem();
                            item.setService(service);
                            item.setQuantity(request.cantidad());
                            item.setAppliedUnitPrice(service.getUnitPrice());
                            booking.addItem(item);
                        });
    }

    // ---------------------------------------------------------------- RF-05

    @Override
    public BookingResponse findById(Long id) {
        return BookingResponse.from(get(id));
    }

    @Override
    public List<BookingResponse> search(Long memberId, Long roomId, LocalDate date, BookingStatus status) {
        boolean byMember = memberId != null;
        boolean byRoom = roomId != null || date != null;
        boolean byStatus = status != null;
        long filters = Stream.of(byMember, byRoom, byStatus).filter(f -> f).count();
        if (filters != 1 || (byRoom && (roomId == null || date == null))) {
            throw new InvalidRequestException("Use exactamente un filtro: miembroId, salaId junto con fecha, o estado");
        }
        List<Booking> bookings;
        if (byMember) {
            bookings = bookingRepository.findByMemberIdOrderByStartTime(memberId);
        } else if (byRoom) {
            bookings = bookingRepository.findByRoomIdAndStartTimeBetweenOrderByStartTime(
                    roomId, date.atStartOfDay(), date.atTime(LocalTime.MAX));
        } else {
            bookings = bookingRepository.findByStatusOrderByStartTime(status);
        }
        return bookings.stream().map(BookingResponse::from).toList();
    }

    // ---------------------------------------------------------------- RF-06 / RN-10

    @Override
    @Transactional
    public BookingResponse addService(Long bookingId, BookingItemRequest request) {
        Booking booking = getPending(bookingId);
        addOrIncrease(booking, request);
        recalculateTotals(booking);
        return BookingResponse.from(booking);
    }

    @Override
    @Transactional
    public BookingResponse removeService(Long bookingId, Long serviceId) {
        Booking booking = getPending(bookingId);
        BookingItem item = booking.getItems().stream()
                .filter(i -> i.getService().getId().equals(serviceId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Servicio en la reserva", serviceId));
        booking.removeItem(item);
        recalculateTotals(booking);
        return BookingResponse.from(booking);
    }

    private Booking getPending(Long id) {
        Booking booking = get(id);
        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new BusinessRuleException("RN-10", "Los servicios solo se cambian mientras la reserva está PENDING");
        }
        return booking;
    }

    // ---------------------------------------------------------------- RF-07 / RF-08 / RF-09

    @Override
    @Transactional
    public BookingResponse confirm(Long id) {
        Booking booking = get(id);
        transition(booking, BookingStatus.CONFIRMED);
        return BookingResponse.from(booking);
    }

    @Override
    @Transactional
    public BookingResponse complete(Long id) {
        Booking booking = get(id);
        transition(booking, BookingStatus.COMPLETED);
        return BookingResponse.from(booking);
    }

    /** RN-08: ≥ 24 h sin cargo; entre 24 h y 2 h, 50 % del costo de la sala; < 2 h o ya empezada, no se puede. */
    @Override
    @Transactional
    public BookingResponse cancel(Long id) {
        Booking booking = get(id);
        if (!booking.getStatus().canTransitionTo(BookingStatus.CANCELLED)) {
            throw invalidTransition(booking.getStatus(), BookingStatus.CANCELLED);
        }
        Duration advance = Duration.between(now(), booking.getStartTime());
        if (advance.compareTo(MIN_CANCELLATION) < 0) {
            throw new BusinessRuleException("RN-08", "No se puede cancelar con menos de 2 h de anticipación o si ya empezó");
        }
        BigDecimal fee = advance.compareTo(FREE_CANCELLATION) >= 0
                ? BigDecimal.ZERO.setScale(2)
                : booking.getRoomCost().multiply(LATE_CANCELLATION_RATE).setScale(2, RoundingMode.HALF_UP);
        booking.setCancellationFee(fee);
        booking.setStatus(BookingStatus.CANCELLED);
        return BookingResponse.from(booking);
    }

    private static void transition(Booking booking, BookingStatus next) {
        if (!booking.getStatus().canTransitionTo(next)) {
            throw invalidTransition(booking.getStatus(), next);
        }
        booking.setStatus(next);
    }

    private static BusinessRuleException invalidTransition(BookingStatus from, BookingStatus to) {
        return new BusinessRuleException("RN-09", "Una reserva " + from + " no puede pasar a " + to);
    }

    // ---------------------------------------------------------------- RF-03

    @Override
    public List<RoomResponse> findAvailableRooms(Long locationId, LocalDate date, LocalTime startTime, LocalTime endTime,
                                                 Integer minCapacity, List<Long> equipmentIds) {
        Location location = locationRepository.findById(locationId)
                .orElseThrow(() -> new ResourceNotFoundException("Sede", locationId));
        if (!startTime.isBefore(endTime)) {
            throw new InvalidRequestException("horaInicio debe ser anterior a horaFin");
        }
        if (startTime.isBefore(location.getOpeningTime()) || endTime.isAfter(location.getClosingTime())) {
            return List.of(); // fuera del horario de la sede no hay salas libres
        }
        LocalDateTime start = date.atTime(startTime);
        LocalDateTime end = date.atTime(endTime);
        Set<Long> busyRoomIds = bookingRepository
                .findByRoom_Location_IdAndStatusInAndStartTimeLessThanAndEndTimeGreaterThan(locationId, ACTIVE, end, start)
                .stream()
                .map(b -> b.getRoom().getId())
                .collect(Collectors.toSet());
        List<Long> requiredEquipment = equipmentIds == null ? List.of() : equipmentIds;

        return roomRepository.findByLocationId(locationId).stream()
                .filter(Room::isActive)
                .filter(r -> !busyRoomIds.contains(r.getId()))
                .filter(r -> minCapacity == null || r.getCapacity() >= minCapacity)
                .filter(r -> r.getEquipment().stream().map(Equipment::getId).collect(Collectors.toSet())
                        .containsAll(requiredEquipment))
                .map(RoomResponse::from)
                .toList();
    }

    // ---------------------------------------------------------------- RF-10

    @Override
    public ConsumptionResponse getConsumption(Long memberId, YearMonth month) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Miembro", memberId));
        BigDecimal included = BigDecimal.valueOf(member.getPlan().getIncludedHoursPerMonth()).setScale(2);
        BigDecimal used = usedHours(memberId, month).setScale(2);
        BigDecimal zero = BigDecimal.ZERO.setScale(2);
        return new ConsumptionResponse(memberId, month.toString(), included, used,
                included.subtract(used).max(zero), used.subtract(included).max(zero));
    }

    // ----------------------------------------------------------------

    private Booking get(Long id) {
        return bookingRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Reserva", id));
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }
}
