package com.academy.coworkhub.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class AppConfig {

    private final String zoneId;
    private final String jwtSecret;
    private final int jwtExpirationMinutes;

    public AppConfig(@Value("${coworkhub.zone-id}") String zoneId,
                     @Value("${coworkhub.jwt.secret}") String jwtSecret,
                     @Value("${coworkhub.jwt.expiration-minutes}") int jwtExpirationMinutes) {
        this.zoneId = zoneId;
        this.jwtSecret = jwtSecret;
        this.jwtExpirationMinutes = jwtExpirationMinutes;
    }

    // Falla el arranque si la configuración es inválida, en vez de fallar en el primer login
    @PostConstruct
    void validateConfiguration() {
        ZoneId.of(zoneId);
        if (jwtSecret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("coworkhub.jwt.secret debe tener al menos 32 bytes (HS256)");
        }
        if (jwtExpirationMinutes <= 0) {
            throw new IllegalStateException("coworkhub.jwt.expiration-minutes debe ser mayor que 0");
        }
    }

    @Bean
    public Clock clock() {
        return Clock.system(ZoneId.of(zoneId));
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // JWT HS256: el mismo secreto firma (login) y valida (cada solicitud)
    @Bean
    public JwtEncoder jwtEncoder() {
        return new NimbusJwtEncoder(new ImmutableSecret<>(jwtKey()));
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        return NimbusJwtDecoder.withSecretKey(jwtKey()).macAlgorithm(MacAlgorithm.HS256).build();
    }

    private SecretKey jwtKey() {
        return new SecretKeySpec(jwtSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }
}
