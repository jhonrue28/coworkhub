package com.academy.coworkhub.business;

import com.academy.coworkhub.business.exception.BusinessRuleException;
import com.academy.coworkhub.business.exception.ResourceNotFoundException;
import com.academy.coworkhub.business.service.*;
import com.academy.coworkhub.persistence.entity.MemberStatus;
import com.academy.coworkhub.persistence.entity.Role;
import com.academy.coworkhub.persistence.entity.RoomType;
import com.academy.coworkhub.persistence.repository.UserAccountRepository;
import com.academy.coworkhub.presentation.dto.request.*;
import com.academy.coworkhub.presentation.dto.response.MemberResponse;
import com.academy.coworkhub.presentation.dto.response.RoomResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class CatalogAndMemberServiceTest {

    @Autowired LocationService locationService;
    @Autowired RoomService roomService;
    @Autowired EquipmentService equipmentService;
    @Autowired AdditionalServiceService additionalServiceService;
    @Autowired MembershipPlanService planService;
    @Autowired MemberService memberService;
    @Autowired UserAccountRepository userAccountRepository;
    @Autowired PasswordEncoder passwordEncoder;

    private static String code(Runnable action) {
        return assertThrows(BusinessRuleException.class, action::run).getCode();
    }

    @Test
    void escenario14_filtraMiembrosSuspendidos() {
        var suspended = memberService.search(null, MemberStatus.SUSPENDED);
        assertFalse(suspended.isEmpty());
        assertTrue(suspended.stream().allMatch(m -> m.estado() == MemberStatus.SUSPENDED));
        assertEquals(1, memberService.search("góm", null).size());
    }

    @Test
    void rn11_nombresDuplicados() {
        assertEquals("RN-11", code(() -> equipmentService.create(new EquipmentRequest("proyector"))));
        assertEquals("RN-11", code(() -> additionalServiceService.create(new AdditionalServiceRequest("CATERING", BigDecimal.ONE))));
        assertEquals("RN-11", code(() -> planService.create(new MembershipPlanRequest("Básico", 5, 0, 1))));
        assertEquals("RN-11", code(() -> roomService.create(
                new RoomRequest("Sala Andes", RoomType.MEETING_ROOM, 4, BigDecimal.TEN, true, 1L, Set.of()))));
        // Mismo nombre en otra sede sí se permite
        assertNotNull(roomService.create(
                new RoomRequest("Sala Andes", RoomType.MEETING_ROOM, 4, BigDecimal.TEN, true, 2L, Set.of())).id());
    }

    @Test
    void escenario11_y_rn12_noSeEliminaLoQueEstaEnUso() {
        assertEquals("RN-12", code(() -> roomService.delete(1L)));       // sala con reservas
        assertEquals("RN-12", code(() -> locationService.delete(1L)));   // sede con salas
        assertEquals("RN-12", code(() -> planService.delete(1L)));       // plan con miembros
        assertEquals("RN-12", code(() -> equipmentService.delete(1L)));  // equipamiento asignado
        assertEquals("RN-12", code(() -> additionalServiceService.delete(1L))); // servicio usado
        assertEquals("RN-12", code(() -> memberService.delete(1L)));     // miembro con reservas
    }

    @Test
    void salaConEquipamientoYFiltroPorSede() {
        RoomResponse room = roomService.create(
                new RoomRequest("Sala Nueva", RoomType.DESK, 2, new BigDecimal("12000"), null, 2L, Set.of(1L, 3L)));
        assertTrue(room.activa());
        assertEquals(2, room.equipamiento().size());
        assertTrue(roomService.findAll(2L).stream().allMatch(r -> r.sedeId().equals(2L)));
        assertThrows(ResourceNotFoundException.class, () -> roomService.create(
                new RoomRequest("Otra", RoomType.DESK, 1, BigDecimal.ONE, true, 1L, Set.of(99L))));
        roomService.delete(room.id());
        assertThrows(ResourceNotFoundException.class, () -> roomService.findById(room.id()));
    }

    @Test
    void registraMiembroConSuCuenta() {
        MemberResponse created = memberService.create(new MemberCreateRequest(
                "9090909090", "Marta Díaz", "marta@correo.com", 2L, "marta", "secreta1"));
        assertEquals(MemberStatus.ACTIVE, created.estado());

        var account = userAccountRepository.findByMemberId(created.id()).orElseThrow();
        assertEquals(Role.MEMBER, account.getRole());
        assertEquals("marta", account.getUsername());
        assertTrue(passwordEncoder.matches("secreta1", account.getPassword()));

        assertEquals("RN-11", code(() -> memberService.create(new MemberCreateRequest(
                "9090909090", "Otra", "otra@correo.com", 1L, "otra", "secreta1"))));
        assertEquals("RN-11", code(() -> memberService.create(new MemberCreateRequest(
                "1111111111", "Otra", "otra@correo.com", 1L, "ana", "secreta1"))));

        assertEquals(MemberStatus.SUSPENDED, memberService.changeStatus(created.id(), MemberStatus.SUSPENDED).estado());
        assertEquals(MemberStatus.ACTIVE, memberService.changeStatus(created.id(), MemberStatus.ACTIVE).estado());

        memberService.delete(created.id());
        assertTrue(userAccountRepository.findByUsername("marta").isEmpty());
    }
}
