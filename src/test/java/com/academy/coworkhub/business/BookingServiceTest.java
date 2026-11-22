package com.academy.coworkhub.business;

import com.academy.coworkhub.business.exception.BusinessRuleException;
import com.academy.coworkhub.business.exception.InvalidRequestException;
import com.academy.coworkhub.business.service.BookingService;
import com.academy.coworkhub.business.service.MemberService;
import com.academy.coworkhub.business.service.MembershipPlanService;
import com.academy.coworkhub.persistence.entity.BookingStatus;
import com.academy.coworkhub.presentation.dto.request.BookingItemRequest;
import com.academy.coworkhub.presentation.dto.request.BookingRequest;
import com.academy.coworkhub.presentation.dto.request.MemberCreateRequest;
import com.academy.coworkhub.presentation.dto.request.MembershipPlanRequest;
import com.academy.coworkhub.presentation.dto.response.BookingResponse;
import com.academy.coworkhub.presentation.dto.response.ConsumptionResponse;
import com.academy.coworkhub.presentation.dto.response.RoomResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;

import static com.academy.coworkhub.business.FixedClockConfig.TODAY;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Reglas de reserva con "ahora" = hoy 06:00. Cada test se revierte.
 * Salas de import.sql: 1 Sala Andes (sede 1, 50000/h, cap 8), 2 Oficina Caribe (sede 1, 35000/h, cap 4),
 * 3 Escritorio (sede 1, cap 1), 6 Sala Llanos (inactiva). Sede 1 abre de 07:00 a 21:00.
 */
@SpringBootTest
@Transactional
@Import(FixedClockConfig.class)
class BookingServiceTest {

    @Autowired BookingService bookingService;
    @Autowired MembershipPlanService planService;
    @Autowired MemberService memberService;

    /** Miembro con plan de 2 h incluidas y 50 % de descuento en las excedentes. */
    private Long memberId;

    @BeforeEach
    void createMember() {
        Long planId = planService.create(new MembershipPlanRequest("Plan prueba", 2, 50, 5)).id();
        memberId = memberService.create(new MemberCreateRequest(
                "7777777777", "Miembro Prueba", "prueba@correo.com", planId, "prueba", "secreta1")).id();
    }

    private BookingResponse book(Long roomId, Long member, String from, String to, BookingItemRequest... services) {
        return bookingService.create(new BookingRequest(roomId, member,
                TODAY.atTime(LocalTime.parse(from)), TODAY.atTime(LocalTime.parse(to)), 1, List.of(services)));
    }

    private static String code(Runnable action) {
        return assertThrows(BusinessRuleException.class, action::run).getCode();
    }

    @Test
    void escenario1_reservaConDosServiciosDevuelveElCosto() {
        // 3 h: 2 incluidas + 1 excedente × 50000 × 50 % = 25000; servicios: 25000 + 15000
        BookingResponse b = book(1L, memberId, "09:00", "12:00",
                new BookingItemRequest(1L, 1), new BookingItemRequest(2L, 1));
        assertEquals(BookingStatus.PENDING, b.estado());
        assertEquals(new BigDecimal("25000.00"), b.costoSala());
        assertEquals(new BigDecimal("40000.00"), b.costoServicios());
        assertEquals(new BigDecimal("65000.00"), b.costoTotal());
        assertEquals(2, b.servicios().size());
    }

    @Test
    void escenarios2y3_cruceDeHorarios() {
        book(1L, memberId, "09:00", "11:00");
        assertEquals("RN-01", code(() -> book(1L, memberId, "10:00", "11:00")));
        assertEquals("RN-01", code(() -> book(1L, memberId, "08:00", "09:30")));
        // Termina justo cuando empieza la otra: no se cruzan
        assertNotNull(book(1L, memberId, "11:00", "12:00").id());
        assertNotNull(book(1L, memberId, "08:00", "09:00").id());
    }

    @Test
    void escenario4_duracionYHorario() {
        assertEquals("RN-02", code(() -> book(2L, memberId, "10:00", "10:45")));  // 45 min
        assertEquals("RN-02", code(() -> book(2L, memberId, "10:00", "10:15")));  // < 30 min
        assertEquals("RN-02", code(() -> book(2L, memberId, "08:00", "16:30")));  // > 8 h
        assertEquals("RN-02", code(() -> bookingService.create(new BookingRequest(2L, memberId,
                TODAY.atTime(20, 0), TODAY.plusDays(1).atTime(8, 0), 1, null)))); // dos días
        assertEquals("RN-03", code(() -> book(2L, memberId, "20:30", "21:30")));  // después del cierre
        assertEquals("RN-03", code(() -> book(2L, memberId, "06:30", "07:30")));  // antes de abrir
        assertEquals("RN-03", code(() -> bookingService.create(new BookingRequest(2L, memberId,
                TODAY.minusDays(1).atTime(10, 0), TODAY.minusDays(1).atTime(11, 0), 1, null)))); // pasada
        assertNotNull(book(2L, memberId, "07:00", "08:00").id());               // justo 1 h de anticipación
    }

    @Test
    void rn04_rn05_rn06() {
        assertEquals("RN-04", code(() -> book(6L, memberId, "10:00", "11:00")));  // sala inactiva
        assertEquals("RN-04", code(() -> bookingService.create(new BookingRequest(3L, memberId,
                TODAY.atTime(10, 0), TODAY.atTime(11, 0), 2, null))));               // supera capacidad 1
        assertEquals("RN-05", code(() -> book(2L, 4L, "10:00", "11:00")));        // Pedro está suspendido
        assertEquals("RN-06", code(() -> book(2L, 1L, "10:00", "11:00")));        // Ana ya tiene 2 de 2
    }

    @Test
    void escenario5_cancelarCon24hOMasNoTieneCargo() {
        // Reserva 15 de import.sql: mañana 16:00 (34 h de anticipación)
        BookingResponse cancelled = bookingService.cancel(15L);
        assertEquals(BookingStatus.CANCELLED, cancelled.estado());
        assertEquals(0, cancelled.cargoCancelacion().signum());
    }

    @Test
    void escenario6_cancelarEntre24hY2hCobraElCincuentaPorCiento() {
        // 11:00 es 5 h después de "ahora". 3 h en sala 2: 1 excedente × 35000 × 50 % = 17500
        BookingResponse b = book(2L, memberId, "11:00", "14:00");
        assertEquals(new BigDecimal("17500.00"), b.costoSala());
        assertEquals(new BigDecimal("8750.00"), bookingService.cancel(b.id()).cargoCancelacion());
    }

    @Test
    void escenario7_y_8_cancelacionTardiaYTransicionesInvalidas() {
        BookingResponse b = book(2L, memberId, "07:00", "08:00");
        assertEquals("RN-08", code(() -> bookingService.cancel(b.id())));           // 1 h de anticipación

        BookingResponse other = book(2L, memberId, "12:00", "13:00");
        bookingService.cancel(other.id());
        assertEquals("RN-09", code(() -> bookingService.confirm(other.id())));      // confirmar cancelada
        assertEquals("RN-09", code(() -> bookingService.cancel(other.id())));
        assertEquals("RN-09", code(() -> bookingService.complete(b.id())));         // PENDING → COMPLETED

        assertEquals(BookingStatus.CONFIRMED, bookingService.confirm(b.id()).estado());
        assertEquals(BookingStatus.COMPLETED, bookingService.complete(b.id()).estado());
        assertEquals("RN-09", code(() -> bookingService.confirm(b.id())));
    }

    @Test
    void rf06_rn10_serviciosSoloEnPending() {
        BookingResponse b = book(2L, memberId, "09:00", "10:00", new BookingItemRequest(1L, 1));
        BookingResponse updated = bookingService.addService(b.id(), new BookingItemRequest(1L, 2));
        assertEquals(3, updated.servicios().get(0).cantidad());                     // mismo servicio: suma
        assertEquals(new BigDecimal("75000.00"), updated.costoServicios());
        updated = bookingService.addService(b.id(), new BookingItemRequest(3L, 1));
        assertEquals(new BigDecimal("105000.00"), updated.costoTotal());
        updated = bookingService.removeService(b.id(), 1L);
        assertEquals(new BigDecimal("30000.00"), updated.costoServicios());

        assertEquals("RN-10", code(() -> bookingService.addService(8L, new BookingItemRequest(2L, 1)))); // confirmada
        bookingService.confirm(b.id());
        assertEquals("RN-10", code(() -> bookingService.removeService(b.id(), 3L)));
    }

    @Test
    void rf10_consumoMensual() {
        book(1L, memberId, "09:00", "12:00");
        BookingResponse cancelled = book(1L, memberId, "13:00", "14:00");
        book(1L, memberId, "15:00", "16:00");
        bookingService.cancel(cancelled.id());                                      // devuelve su hora

        ConsumptionResponse c = bookingService.getConsumption(memberId, YearMonth.from(TODAY));
        assertEquals(new BigDecimal("2.00"), c.horasIncluidas());
        assertEquals(new BigDecimal("4.00"), c.horasUsadas());
        assertEquals(new BigDecimal("0.00"), c.horasRestantes());
        assertEquals(new BigDecimal("2.00"), c.horasExcedentes());
    }

    @Test
    void rf03_disponibilidad() {
        LocalDate in2days = TODAY.plusDays(2); // la sala 1 está ocupada de 10:00 a 13:00 (reservas 8 y 9)
        List<Long> free = bookingService.findAvailableRooms(1L, in2days, LocalTime.of(10, 0), LocalTime.of(12, 0), null, null)
                .stream().map(RoomResponse::id).toList();
        assertEquals(List.of(2L, 3L), free.stream().sorted().toList());

        assertEquals(List.of(2L), bookingService.findAvailableRooms(1L, in2days, LocalTime.of(10, 0), LocalTime.of(12, 0), null, List.of(3L))
                .stream().map(RoomResponse::id).toList());
        assertTrue(bookingService.findAvailableRooms(1L, in2days, LocalTime.of(10, 0), LocalTime.of(12, 0), 5, null).isEmpty());
        assertTrue(bookingService.findAvailableRooms(1L, in2days, LocalTime.of(13, 0), LocalTime.of(14, 0), 5, null)
                .stream().anyMatch(r -> r.id() == 1L));
        assertTrue(bookingService.findAvailableRooms(1L, in2days, LocalTime.of(5, 0), LocalTime.of(6, 0), null, null).isEmpty());
        // La sala inactiva 6 nunca aparece
        assertTrue(bookingService.findAvailableRooms(2L, in2days, LocalTime.of(8, 0), LocalTime.of(9, 0), null, null)
                .stream().noneMatch(r -> r.id() == 6L));
    }

    @Test
    void rf05_listadosConUnSoloFiltro() {
        assertTrue(bookingService.search(1L, null, null, null).stream().allMatch(b -> b.miembroId() == 1L));
        assertEquals(2, bookingService.search(null, 1L, TODAY.plusDays(2), null).size());
        assertTrue(bookingService.search(null, null, null, BookingStatus.CANCELLED).stream()
                .allMatch(b -> b.estado() == BookingStatus.CANCELLED));
        assertThrows(InvalidRequestException.class, () -> bookingService.search(null, null, null, null));
        assertThrows(InvalidRequestException.class, () -> bookingService.search(1L, null, null, BookingStatus.PENDING));
        assertThrows(InvalidRequestException.class, () -> bookingService.search(null, 1L, null, null));
    }
}
