package com.academy.coworkhub.business;

import com.academy.coworkhub.business.exception.BusinessRuleException;
import com.academy.coworkhub.business.service.BookingService;
import com.academy.coworkhub.persistence.repository.BookingRepository;
import com.academy.coworkhub.presentation.dto.request.BookingRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static com.academy.coworkhub.business.FixedClockConfig.TODAY;
import static org.junit.jupiter.api.Assertions.assertEquals;

/** Escenario 13: dos solicitudes simultáneas para la misma sala y horario → una 201 y la otra 409 RN-01. */
@SpringBootTest
@Import(FixedClockConfig.class)
class ConcurrentBookingTest {

    @Autowired BookingService bookingService;
    @Autowired BookingRepository bookingRepository;

    private final List<Long> created = new CopyOnWriteArrayList<>();

    @AfterEach
    void cleanUp() {
        // Sin @Transactional (cada hilo tiene su transacción): se borran a mano
        bookingRepository.deleteAllById(created);
    }

    @Test
    void escenario13_solicitudesSimultaneas() throws Exception {
        LocalDateTime start = TODAY.plusDays(10).atTime(10, 0);
        // Dos miembros distintos (Laura y Sofía): solo el bloqueo de la sala puede evitar el cruce
        List<Long> members = List.of(3L, 5L);
        CountDownLatch ready = new CountDownLatch(members.size());
        CountDownLatch go = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(members.size());

        List<Future<String>> results = new ArrayList<>();
        for (Long member : members) {
            results.add(pool.submit(() -> {
                ready.countDown();
                go.await();
                try {
                    created.add(bookingService.create(new BookingRequest(2L, member, start, start.plusHours(1), 1, null)).id());
                    return "201";
                } catch (BusinessRuleException e) {
                    return e.getCode();
                }
            }));
        }
        ready.await();
        go.countDown();

        List<String> outcomes = new ArrayList<>();
        for (Future<String> f : results) {
            outcomes.add(f.get(30, TimeUnit.SECONDS));
        }
        pool.shutdown();

        assertEquals(List.of("201", "RN-01"), outcomes.stream().sorted().toList());
    }
}
