package com.academy.coworkhub.persistence;

import com.academy.coworkhub.persistence.entity.Booking;
import com.academy.coworkhub.persistence.entity.BookingStatus;
import com.academy.coworkhub.persistence.entity.MemberStatus;
import com.academy.coworkhub.persistence.entity.Role;
import com.academy.coworkhub.persistence.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

// Verifica que import.sql cumpla los mínimos del requerimiento (sección 6)
@SpringBootTest
class ImportDataTest {

    @Autowired LocationRepository locations;
    @Autowired RoomRepository rooms;
    @Autowired EquipmentRepository equipment;
    @Autowired AdditionalServiceRepository services;
    @Autowired MembershipPlanRepository plans;
    @Autowired MemberRepository members;
    @Autowired UserAccountRepository users;
    @Autowired BookingRepository bookings;
    @Autowired PasswordEncoder passwordEncoder;

    @Test
    void cargaLosMinimosDelRequerimiento() {
        assertTrue(locations.count() >= 2);
        assertTrue(equipment.count() >= 4);
        assertTrue(services.count() >= 3);
        assertTrue(plans.count() >= 3);

        var allRooms = rooms.findAll();
        assertTrue(allRooms.size() >= 6);
        assertTrue(allRooms.stream().anyMatch(r -> !r.isActive()));
        // findAll trae sede y equipamiento con @EntityGraph: se leen fuera de la transacción sin LazyInitializationException
        assertTrue(allRooms.stream().allMatch(r -> r.getLocation().getName() != null && !r.getEquipment().isEmpty()));

        assertTrue(members.count() >= 5);
        assertFalse(members.search(null, MemberStatus.SUSPENDED).isEmpty());

        var accounts = users.findAll();
        assertEquals(4, accounts.size());
        assertEquals(Set.of(Role.values()), accounts.stream().map(u -> u.getRole()).collect(Collectors.toSet()));
        assertTrue(passwordEncoder.matches("ana123", users.findByUsername("ana").orElseThrow().getPassword()));
        assertTrue(users.findByMemberId(1L).isPresent());
        assertTrue(users.findByUsername("admin").orElseThrow().getMember() == null);

        assertTrue(bookings.count() >= 15);
        assertEquals(Set.of(BookingStatus.values()),
                Arrays.stream(BookingStatus.values()).filter(s -> !bookings.findByStatusOrderByStartTime(s).isEmpty()).collect(Collectors.toSet()));
    }

    @Test
    void servicesCostCoincideConLosDetalles() {
        for (BookingStatus status : BookingStatus.values()) {
            List<Booking> list = bookings.findByStatusOrderByStartTime(status);
            for (Booking b : list) {
                BigDecimal sum = b.getItems().stream()
                        .map(i -> i.getAppliedUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                assertEquals(0, sum.compareTo(b.getServicesCost()), "reserva " + b.getId());
                assertEquals(0, b.getRoomCost().add(b.getServicesCost()).compareTo(b.getTotalCost()), "reserva " + b.getId());
            }
        }
    }
}
