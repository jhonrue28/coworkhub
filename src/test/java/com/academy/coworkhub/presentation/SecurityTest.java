package com.academy.coworkhub.presentation;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Roles y dueño del recurso (RN-13). En import.sql: ana es el miembro 1 (reserva 1) y carlos el 2 (reserva 2). */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SecurityTest {

    @Autowired MockMvc mvc;

    @Autowired JwtEncoder jwtEncoder;

    private String login(String username, String password) throws Exception {
        String body = mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo").value("Bearer"))
                .andReturn().getResponse().getContentAsString();
        return body.replaceAll(".*\"token\":\"([^\"]+)\".*", "$1");
    }

    @Test
    void login_devuelveJwtYPerfil() throws Exception {
        String token = login("ana", "ana123");
        mvc.perform(get("/auth/perfil").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("ana"))
                .andExpect(jsonPath("$.role").value("MEMBER"))
                .andExpect(jsonPath("$.miembroId").value(1));

        String admin = login("admin", "admin123");
        mvc.perform(get("/auth/perfil").header("Authorization", "Bearer " + admin))
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.miembroId").doesNotExist());
        // el rol del token se aplica en @PreAuthorize
        mvc.perform(get("/api/miembros").header("Authorization", "Bearer " + admin)).andExpect(status().isOk());
        mvc.perform(get("/api/miembros").header("Authorization", "Bearer " + token)).andExpect(status().isForbidden());
    }

    @Test
    void login_credencialesIncorrectas_401() throws Exception {
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"incorrecta\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NO_AUTENTICADO"));
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"nadie\",\"password\":\"x\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void escenario10_sinTokenOAlterado_401() throws Exception {
        mvc.perform(get("/api/sedes"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NO_AUTENTICADO"));

        String token = login("admin", "admin123");
        String altered = token.substring(0, token.length() - 2) + (token.endsWith("AA") ? "BB" : "AA");
        mvc.perform(get("/api/sedes").header("Authorization", "Bearer " + altered))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NO_AUTENTICADO"));
    }

    @Test
    void escenario10_tokenVencido_401() throws Exception {
        Instant past = Instant.now().minus(2, ChronoUnit.HOURS);
        JwtClaimsSet claims = JwtClaimsSet.builder().subject("admin").claim("role", "ADMIN")
                .issuedAt(past).expiresAt(past.plus(60, ChronoUnit.MINUTES)).build();
        String expired = jwtEncoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
        mvc.perform(get("/api/sedes").header("Authorization", "Bearer " + expired))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void publicos_sinCredenciales() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }

    @Test
    @WithUserDetails("ana")
    void escenario9_miembroConsultaReservaAjena_403() throws Exception {
        mvc.perform(get("/api/reservas/2"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("ACCESO_DENEGADO"));
        mvc.perform(get("/api/reservas/1")).andExpect(status().isOk());
    }

    @Test
    @WithUserDetails("ana")
    void miembro_soloSusDatos() throws Exception {
        mvc.perform(get("/api/miembros/1")).andExpect(status().isOk());
        mvc.perform(get("/api/miembros/1/consumo")).andExpect(status().isOk());
        mvc.perform(get("/api/reservas").param("miembroId", "1")).andExpect(status().isOk());

        mvc.perform(get("/api/miembros/2")).andExpect(status().isForbidden());
        mvc.perform(get("/api/miembros/2/consumo")).andExpect(status().isForbidden());
        mvc.perform(get("/api/miembros")).andExpect(status().isForbidden());
        mvc.perform(get("/api/reservas").param("miembroId", "2")).andExpect(status().isForbidden());
        mvc.perform(get("/api/reservas").param("estado", "PENDING")).andExpect(status().isForbidden());
        mvc.perform(post("/api/reservas/1/confirmar")).andExpect(status().isForbidden());
        mvc.perform(post("/api/reservas").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"salaId\":1,\"miembroId\":2,\"inicio\":\"2030-06-03T10:00:00\",\"fin\":\"2030-06-03T11:00:00\",\"asistentes\":1}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithUserDetails("ana")
    void miembro_leeCatalogosPeroNoLosModifica() throws Exception {
        mvc.perform(get("/api/sedes")).andExpect(status().isOk());
        mvc.perform(delete("/api/equipamientos/1")).andExpect(status().isForbidden());
    }

    @Test
    @WithUserDetails("recepcion")
    void recepcion_gestionaMiembrosPeroNoCatalogos() throws Exception {
        mvc.perform(get("/api/miembros")).andExpect(status().isOk());
        mvc.perform(get("/api/reservas/2")).andExpect(status().isOk());
        mvc.perform(post("/api/equipamientos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Parlante\"}"))
                .andExpect(status().isForbidden());
    }
}
