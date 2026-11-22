package com.academy.coworkhub.business;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

/** "Ahora" fijo: hoy a las 06:00 en Bogotá, el mismo "hoy" que usa import.sql. */
@TestConfiguration
public class FixedClockConfig {

    public static final ZoneId ZONE = ZoneId.of("America/Bogota");
    public static final LocalDate TODAY = LocalDate.now(ZONE);

    @Bean
    @Primary
    Clock fixedClock() {
        return Clock.fixed(TODAY.atTime(6, 0).atZone(ZONE).toInstant(), ZONE);
    }
}
